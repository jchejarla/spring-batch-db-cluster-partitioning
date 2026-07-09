/*
 * Copyright 2025 Janardhan Chejarla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.jchejarla.springbatch.clustering.partition;

import io.github.jchejarla.springbatch.clustering.api.ClusterAwarePartitioner;
import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.core.CoordinationStatus;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import io.github.jchejarla.springbatch.clustering.query.JobPhase;
import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNode;
import io.github.jchejarla.springbatch.clustering.polling.PartitionAssignmentTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
 * <p>Also see {@link ClusterAwarePartitioner}
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

    // The per-job completion/orphan monitors below block (sleep-loops polling the DB). Run them on
    // virtual threads (one per task) rather than the shared ForkJoinPool.commonPool, so a master running
    // many concurrent jobs cannot exhaust the common pool and stall monitoring.
    private final Executor monitorExecutor = Executors.newVirtualThreadPerTaskExecutor();
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
        long jobExecutionId = managerStepExecution.getJobExecutionId();
        recordPhase(jobExecutionId, JobPhase.RECEIVED);
        log.info("ClusterAwarePartitionHandler is invoked to handle the partition and execution of the partitioned task : {}", managerStepExecution.getStepName());
        Set<StepExecution> stepExecutions = stepSplitter.split(managerStepExecution, 0);
        log.info("Number of partition steps generated are : {} ", stepExecutions.size());
        if(stepExecutions.isEmpty()) {
            log.warn("Partitioner returned empty set of executions, so there is nothing to distribute");
            return  stepExecutions;
        }
        recordPhase(jobExecutionId, JobPhase.PARTITIONED);
        Long masterStepExecutionId = managerStepExecution.getId();
        List<Object[]> params = new ArrayList<>();

        for(StepExecution stepExecution: stepExecutions) {
            Long stepExecutionId = stepExecution.getId();
            String partitionKey = stepExecution.getStepName();
            String nodeId = stepExecution.getExecutionContext().getString(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER);
            PartitionTransferableProp isTaskTransferable = stepExecution.getExecutionContext().get(ClusterPartitioningConstants.IS_TRANSFERABLE_IDENTIFIER, PartitionTransferableProp.class);
            params.add(new Object[]{stepExecutionId, jobExecutionId, partitionKey, nodeId, PartitionStatus.PENDING.name(), masterStepExecutionId, Objects.equals(PartitionTransferableProp.YES, isTaskTransferable) ? 1 : 0});
        }

        log.info("Persisting master step info into coordination table with status = CREATED, master step execution id {}", masterStepExecutionId);
        databaseBackedClusterService.saveBatchJobCoordinationInfo(jobExecutionId, masterStepExecutionId, stepSplitter.getStepName());
        log.info("Persisting of master step info into coordination table with status = CREATED is completed, master step execution id {}", masterStepExecutionId);

        log.info("Persisting workload distribution for step {} started", managerStepExecution.getStepName());
        databaseBackedClusterService.saveBatchPartitions(params);
        log.info("Persisting workload distribution for step {} completed, now cluster wide nodes will pickup the workload and run them", managerStepExecution.getStepName());
        recordPhase(jobExecutionId, JobPhase.DISTRIBUTED);

        log.info("Updating master step info into coordination table with status = STARTED, master step execution id {}", masterStepExecutionId);
        databaseBackedClusterService.updateBatchJobCoordinationStatus(jobExecutionId, masterStepExecutionId, CoordinationStatus.STARTED.name());
        log.info("Updating of master step info into coordination table with status = STARTED is completed, master step execution id {}", masterStepExecutionId);

        // PartitionHandler need to wait (synchronously) until all the tasks are complete, if this method returns, then the job is completed
        waitForExecutionOfAllTasks(managerStepExecution.getId());
        recordPhase(jobExecutionId, JobPhase.COMPLETION_DETECTED);

        log.info("Updating master step info into coordination table with status = COMPLETED, master step execution id {}", masterStepExecutionId);
        databaseBackedClusterService.updateBatchJobCoordinationStatus(jobExecutionId, masterStepExecutionId, CoordinationStatus.COMPLETED.name());
        log.info("Updating of master step info into coordination table with status = COMPLETED is completed, master step execution id {}", masterStepExecutionId);

        return stepExecutions;
    }


    /**
     * Records a master-side coordination phase for observability, only when phase-timing capture is
     * enabled. Never lets an observability failure affect the job.
     */
    private void recordPhase(long jobExecutionId, JobPhase phase) {
        if (!batchClusterProperties.isCapturePhaseTimings()) {
            return;
        }
        try {
            databaseBackedClusterService.recordPhaseEvent(jobExecutionId, phase.name());
        } catch (Exception e) {
            log.warn("Failed to record phase-timing event {} for jobExecutionId={}", phase, jobExecutionId, e);
        }
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
                throw new CompletionException(e);
            } finally {
                areAllTasksCompleted.set(true);
            }
        }, monitorExecutor);

        CompletableFuture<Void> orphanedTasksMonitorTask = CompletableFuture.runAsync(()->{
            try {
                while (!areAllTasksCompleted.get() && !taskCompletionMonitorTask.isDone()) {
                    pollForOrphanedTasksAndReArrange(masterStepExecutionId);
                    Thread.sleep(batchClusterProperties.getOrphanedTasksPollingInterval());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Exception occurred while monitoring for orphaned tasks and re-arrange them to different available nodes", e);
                throw new CompletionException("Exception occurred while monitoring for orphaned tasks and re-arrange them to different available nodes", e);
            }
        }, monitorExecutor);
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
        } catch (Exception e) {
            log.error("Exception occurred while waiting for workload partitions executions", e);
            throw new JobExecutionException("Exception occurred while waiting for workload partitions executions", e);
        }
        return false;
    }

    protected void pollForOrphanedTasksAndReArrange(long masterStepExecutionId) throws JobExecutionException {
        List<PartitionAssignmentTask> orphanedTasks = databaseBackedClusterService.checkForOrphanedTasks(masterStepExecutionId);
        if (orphanedTasks.isEmpty()) {
            return;
        }

        // Non-transferable partitions must never run on another node (node-local state or non-idempotent
        // side effects). Their owning node has left the cluster, so they cannot be recovered — fail them
        // so the job fails cleanly rather than being silently re-executed elsewhere or hanging forever.
        List<PartitionAssignmentTask> nonTransferable = orphanedTasks.stream().filter(task -> !task.isTransferable()).toList();
        if (!nonTransferable.isEmpty()) {
            log.error("{} orphaned partition(s) are non-transferable and their node has left the cluster; failing them (they cannot be safely re-executed elsewhere)", nonTransferable.size());
            databaseBackedClusterService.updatePartitionsStatus(nonTransferable, PartitionStatus.FAILED.name());
        }

        List<PartitionAssignmentTask> transferable = orphanedTasks.stream().filter(PartitionAssignmentTask::isTransferable).toList();
        if (transferable.isEmpty()) {
            return;
        }
        log.info("Reassigning {} transferable orphaned partition(s) whose node has left the cluster", transferable.size());
        List<ClusterNode> activeNodes = databaseBackedClusterService.getActiveNodes();
        if (activeNodes.isEmpty()) {
            log.error("There are no active nodes found from database to reassign orphaned partitions");
            throw new JobExecutionException("There are no active nodes found from database to reassign orphaned partitions");
        }

        // re-assign transferable orphaned tasks round-robin across the healthy nodes
        List<Object[]> params = new ArrayList<>();
        int index = 0;
        for (PartitionAssignmentTask originTask : transferable) {
            String assignedToNode = activeNodes.get(index % activeNodes.size()).nodeId();
            index++;
            params.add(new Object[]{assignedToNode, new Date(), originTask.jobExecutionId(), originTask.masterStepExecutionId(), originTask.stepExecutionId()});
        }
        databaseBackedClusterService.updateBatchPartitionsToReAssignedNodes(params);
    }

}
