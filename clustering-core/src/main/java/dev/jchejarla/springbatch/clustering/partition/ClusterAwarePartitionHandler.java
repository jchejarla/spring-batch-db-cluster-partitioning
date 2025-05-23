package dev.jchejarla.springbatch.clustering.partition;

import dev.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import dev.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import dev.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import dev.jchejarla.springbatch.clustering.mgmt.ClusterNode;
import dev.jchejarla.springbatch.clustering.polling.PartitionAssignmentTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A custom {@link PartitionHandler} that coordinates partition execution across a dynamic cluster
 * of Spring Batch nodes using a shared relational database for metadata synchronization.
 *
 * <p>This handler assumes the master role for any job it launches, persisting partition metadata
 * and monitoring execution progress. It enables decentralized task assignment, fault tolerance,
 * and task reassignment in case of node failures.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Autowired
 * ClusterAwarePartitionHandler partitionHandler;
 *
 * @Bean
 * public Step multiNodeExecutionStep(JobRepository jobRepository, PlatformTransactionManager txnManager) {
 *     return new StepBuilder("multiNodeExecStep.manager", jobRepository)
 *             .partitioner("multiNodeExecStep", partitioner)
 *             .partitionHandler(partitionHandler)
 *             .step(multiNodeWorkerStep(jobRepository, txnManager))
 *             .build();
 * }
 * }</pre>
 *
 * <p>Also see {@link dev.jchejarla.springbatch.clustering.api.ClusterAwarePartitioner}
 * for implementing a custom partitioner compatible with this handler.</p>
 *
 * @author Janardhan Chejarla
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnClusterEnabled
public class ClusterAwarePartitionHandler implements PartitionHandler {

    private final DatabaseBackedClusterService databaseBackedClusterService;
    private final BatchClusterProperties batchClusterProperties;
    /**
     * Handles the splitting and coordination of partitioned steps across the cluster.
     * <p>This method registers the current node as the job master, persists partition metadata,
     * and monitors execution until all tasks are complete. It ensures partition execution
     * is coordinated and fault-tolerant across available nodes.</p>
     *
     * @param stepSplitter the splitter that creates individual partition steps
     * @param managerStepExecution the master step execution initiating the partitioning
     * @return a collection of completed {@link StepExecution} instances
     * @throws Exception if coordination or execution monitoring fails
     */
    @Override
    public Collection<StepExecution> handle(StepExecutionSplitter stepSplitter, StepExecution managerStepExecution) throws Exception {
        log.info("ClusterAwarePartitionHandler is invoked to handle the partition and execution of the partitioned task : {}", managerStepExecution.getStepName());
        Set<StepExecution> stepExecutions = stepSplitter.split(managerStepExecution, 0);
        log.info("Number of partition steps generated are : {} ", stepExecutions.size());
        if(stepExecutions.isEmpty()) {
            log.warn("Partitioner returned empty set of executions, so there is nothing to distribute");
            return  stepExecutions;
        }
        Long masterStepExecutionId = managerStepExecution.getId();
        long jobExecutionId = managerStepExecution.getJobExecutionId();
        List<Object[]> params = new ArrayList<>();

        for(StepExecution stepExecution: stepExecutions) {
            Long stepExecutionId = stepExecution.getId();
            String partitionKey = stepExecution.getStepName();
            String nodeId = stepExecution.getExecutionContext().getString(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER);
            PartitionTransferableProp isTaskTransferable = stepExecution.getExecutionContext().get(ClusterPartitioningConstants.IS_TRANSFERABLE_IDENTIFIER, PartitionTransferableProp.class);
            params.add(new Object[]{stepExecutionId, jobExecutionId, partitionKey, nodeId, "PENDING", masterStepExecutionId, Objects.equals(PartitionTransferableProp.YES, isTaskTransferable) ? 1 : 0});
        }

        log.info("Persisting master step info into coordination table with status = CREATED, master step execution id {}", masterStepExecutionId);
        databaseBackedClusterService.saveBatchJobCoordinationInfo(jobExecutionId, masterStepExecutionId, stepSplitter.getStepName());
        log.info("Persisting of master step info into coordination table with status = CREATED is completed, master step execution id {}", masterStepExecutionId);

        log.info("Persisting workload distribution for step {} started", managerStepExecution.getStepName());
        databaseBackedClusterService.saveBatchPartitions(params);
        log.info("Persisting workload distribution for step {} completed, now cluster wide nodes will pickup the workload and run them", managerStepExecution.getStepName());

        log.info("Updating master step info into coordination table with status = STARTED, master step execution id {}", masterStepExecutionId);
        databaseBackedClusterService.updateBatchJobCoordinationStatus(jobExecutionId, masterStepExecutionId, "STARTED");
        log.info("Updating of master step info into coordination table with status = STARTED is completed, master step execution id {}", masterStepExecutionId);

        // PartitionHandler need to wait (synchronously) until all the tasks are complete, if this method returns, then the job is completed
        waitForExecutionOfAllTasks(managerStepExecution.getId());

        log.info("Updating master step info into coordination table with status = COMPLETED, master step execution id {}", masterStepExecutionId);
        databaseBackedClusterService.updateBatchJobCoordinationStatus(jobExecutionId, masterStepExecutionId, "COMPLETED");
        log.info("Updating of master step info into coordination table with status = COMPLETED is completed, master step execution id {}", masterStepExecutionId);

        return stepExecutions;
    }


    private void waitForExecutionOfAllTasks(final long masterStepExecutionId) {

        final AtomicBoolean areAllTasksCompleted = new AtomicBoolean(false);

        CompletableFuture<Void> taskCompletionMonitorTask = CompletableFuture.runAsync(()->{
            try {
                while(!areAllTasksCompleted(masterStepExecutionId)) {
                    Thread.sleep(batchClusterProperties.getMasterTaskStatusCheckInterval());
                }
            } catch (Exception e) {
                log.error("Exception occurred while waiting for all tasks to be completed" , e);
                areAllTasksCompleted.set(true);
                throw new CompletionException(e);
            } finally {
                areAllTasksCompleted.set(true);
            }
        });

        CompletableFuture<Void> orphanedTasksMonitorTask = CompletableFuture.runAsync(()->{
            try {
                while (!areAllTasksCompleted.get() && !taskCompletionMonitorTask.isDone()) {
                    pollForOrphanedTasksAndReArrange(masterStepExecutionId);
                }
            } catch (Exception e) {
                log.error("Exception occurred while monitoring for orphaned tasks and re-arrange them to different available nodes");
                throw new CompletionException("Exception occurred while monitoring for orphaned tasks and re-arrange them to different available nodes", e);
            }
        });
        CompletableFuture.allOf(taskCompletionMonitorTask, orphanedTasksMonitorTask).join();
    }

    protected boolean areAllTasksCompleted(long masterStepExecutionId) throws JobExecutionException {
        try {
            int pending = databaseBackedClusterService.getPendingTasksCount(masterStepExecutionId);
            if (pending == 0) {
                return true;
            }
            if (batchClusterProperties.isTracingEnabled()) {
                log.info("There are {} partition tasks waiting to be completed, master step execution id : {}", pending, masterStepExecutionId);
            }
            Thread.sleep(batchClusterProperties.getMasterTaskStatusCheckInterval());
        } catch (Exception e) {
            log.error("Exception occurred while waiting for workload partitions executions", e);
            throw new JobExecutionException("Exception occurred while waiting for workload partitions executions", e);
        }
        return false;
    }

    protected void pollForOrphanedTasksAndReArrange(long masterStepExecutionId) throws JobExecutionException {
        List<PartitionAssignmentTask> orphanedTasks = databaseBackedClusterService.checkForOrphanedTasks(masterStepExecutionId);
        if (!orphanedTasks.isEmpty()) {
            log.info("There are {} orphaned tasks found that are initially assigned to a cluster which is currently not healthy " +
                            "(If the task JVM is still up and health status is not registered, Step tasks will be discarded from that JVM), re assigning them to a different node",
                    orphanedTasks.size());
            List<ClusterNode> activeNodes = databaseBackedClusterService.getActiveNodes();
            if (activeNodes.isEmpty()) {
                log.error("There are no active nodes found from database, please check the log if there are any errors in registering/ updating the heart beat of nodes");
                throw new JobExecutionException("There are no active nodes found from database, please check the log if there are any errors in registering/ updating the heart beat of nodes");
            }

            // re-assign orphaned tasks to active nodes
            // there is co-ordination required on the Node that was executing the task (to avoid to issue where Health
            List<Object[]> params = new ArrayList<>();
            int index = 0;
            for (PartitionAssignmentTask originTask : orphanedTasks) {
                String assignedToNode = activeNodes.get(index).nodeId();
                params.add(new Object[]{assignedToNode, new Date(), originTask.jobExecutionId(), originTask.masterStepExecutionId(), originTask.stepExecutionId()});
            }
            databaseBackedClusterService.updateBatchPartitionsToReAssignedNodes(params);
        }
    }

}
