package dev.jchejarla.springbatch.clustering.polling;

import dev.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import dev.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import dev.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * This class is responsible for polling the database for partition tasks assigned to the current node
 * and executing them. It runs as a background process, periodically checking for new tasks,
 * claiming them, and then executing the corresponding Spring Batch steps.
 *
 * @author Janardhan Chejarla
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnClusterEnabled
public class PartitionedWorkerNodeTasksRunner {

    private final ApplicationContext applicationContext;
    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;
    private final TaskExecutor taskExecutor;
    private final BatchClusterProperties batchClusterProperties;
    private final DatabaseBackedClusterService databaseBackedClusterService;
    private final TaskScheduler partitionPollingScheduler;

    /**
     * Starts the partition task polling and execution process when the application is ready.
     * This method is triggered by the {@link ApplicationReadyEvent}.  It schedules the
     * {@link #pollAndExecute()} method to run at a fixed rate, as configured in the
     * {@link BatchClusterProperties}.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("Starting monitoring task for partitions");
        partitionPollingScheduler.scheduleAtFixedRate(this::pollAndExecute, Duration.ofMillis(batchClusterProperties.getTaskPollingInterval()));
        log.info("Started monitoring task for partitions");

    }

    /**
     * Polls the database for partition tasks assigned to this node, claims them, and executes them.
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     * <li>Fetches partition tasks assigned to this node from the database.</li>
     * <li>If there are tasks to run, updates their status to "CLAIMED" in the database.</li>
     * <li>Iterates through the tasks and executes each one in a separate thread using the
     * {@link TaskExecutor}.</li>
     * <li>For each task:
     * <ol>
     * <li>Retrieves the {@link JobExecution} and {@link StepExecution} from the {@link JobExplorer}.</li>
     * <li>Sets the step execution status to "STARTED".</li>
     * <li>Retrieves the {@link Step} bean from the {@link ApplicationContext}.</li>
     * <li>Executes the step.</li>
     * <li>Updates the step execution status to "COMPLETED" or "FAILED", and updates the {@link JobRepository}.</li>
     * <li>Updates the partition task status in the database to "COMPLETED" or "FAILED".</li>
     * </ol>
     * </li>
     * </ol>
     */
    public void pollAndExecute() {
        List<PartitionAssignmentTask> partitionsToRun = databaseBackedClusterService.fetchPartitionAssignedTasks();
        if(!partitionsToRun.isEmpty()) {
            log.info("Updating tasks status to CLAIMED");
            databaseBackedClusterService.updatePartitionsStatus(partitionsToRun, "CLAIMED");
            log.info("Updating tasks status to CLAIMED is completed");
        }
        for (PartitionAssignmentTask partitionAssignmentTask : partitionsToRun) {
            taskExecutor.execute(()-> executeStep(partitionAssignmentTask));
        }
    }

    protected void executeStep(PartitionAssignmentTask partitionAssignmentTask) {
        Long stepExecutionId = partitionAssignmentTask.stepExecutionId();
        Long jobExecutionId = partitionAssignmentTask.jobExecutionId();
        try {
            StepExecution stepExecution = jobExplorer.getStepExecution(jobExecutionId, stepExecutionId);
            assert stepExecution != null;
            stepExecution.setStartTime(LocalDateTime.now());
            stepExecution.setStatus(BatchStatus.STARTED);
            jobRepository.update(stepExecution);

            // (assumes each partition runs same Step implementation)
            Step step = applicationContext.getBean(partitionAssignmentTask.masterStepName(), Step.class);

            try {
                // Launch the Step
                step.execute(stepExecution);
                stepExecution.setStatus(BatchStatus.COMPLETED);
                stepExecution.setExitStatus(ExitStatus.COMPLETED);
                databaseBackedClusterService.updatePartitionStatus(partitionAssignmentTask, "COMPLETED");
            } catch (Exception e) {
                stepExecution.setStatus(BatchStatus.FAILED);
                stepExecution.setExitStatus(new ExitStatus("FAILED", e.getMessage()));
                databaseBackedClusterService.updatePartitionStatus(partitionAssignmentTask, "FAILED");
            } finally {
                stepExecution.setEndTime(LocalDateTime.now());
                jobRepository.update(stepExecution);
            }
        } catch (Exception e) {
           log.error("Error occurred while running the tasks on node {}", batchClusterProperties.getNodeId(), e);
           databaseBackedClusterService.updatePartitionStatus(partitionAssignmentTask, "FAILED");
        }
    }

}

