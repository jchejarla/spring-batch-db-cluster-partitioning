package dev.jchejarla.springbatch.clustering.partition;

import dev.jchejarla.springbatch.clustering.BaseUnitTest;
import dev.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import dev.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import dev.jchejarla.springbatch.clustering.mgmt.ClusterNode;
import dev.jchejarla.springbatch.clustering.polling.PartitionAssignmentTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.item.ExecutionContext;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class ClusterAwarePartitionHandlerUnitTest extends BaseUnitTest {

    @Mock
    DatabaseBackedClusterService databaseBackedClusterService;
    @Mock
    BatchClusterProperties batchClusterProperties;
    @Mock
    StepExecutionSplitter stepSplitter;
    @Mock
    StepExecution managerStepExecution;

    ClusterAwarePartitionHandler clusterAwarePartitionHandler;

    @BeforeEach
    public void init() {
        clusterAwarePartitionHandler = new ClusterAwarePartitionHandler(databaseBackedClusterService, batchClusterProperties);
        clusterAwarePartitionHandler = Mockito.spy(clusterAwarePartitionHandler);
    }

    @Test
    public void testHandleWhenPartitionerReturnsEmptyExecutions() throws Exception {
        Collection<StepExecution> stepExecutionCollections = clusterAwarePartitionHandler.handle(stepSplitter, managerStepExecution);
        assertTrue(stepExecutionCollections.isEmpty());
    }

    @Test
    public void testHandleSuccessfullyHandlesPartitions() throws Exception {
        Set<StepExecution> stepExecutions = new HashSet<>();
        StepExecution stepExecution = mock(StepExecution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        stepExecutions.add(stepExecution);
        doReturn(executionContext).when(stepExecution).getExecutionContext();
        doReturn("Test-Node-123").when(executionContext).getString(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER);
        doReturn(stepExecutions).when(stepSplitter).split(any(), anyInt());
        doReturn(1, 0).when(databaseBackedClusterService).getPendingTasksCount(anyLong());
        doReturn(true).when(batchClusterProperties).isTracingEnabled();
        Collection<StepExecution> stepExecutionCollections = clusterAwarePartitionHandler.handle(stepSplitter, managerStepExecution);
        assertEquals(1, stepExecutionCollections.size());
        verify(databaseBackedClusterService, times(1)).saveBatchJobCoordinationInfo(anyLong(), anyLong(), any());
        verify(databaseBackedClusterService, times(1)).saveBatchPartitions(any());
        verify(databaseBackedClusterService, times(2)).updateBatchJobCoordinationStatus(anyLong(), anyLong(), anyString());
    }

    @Test
    public void testHandleSuccessfullyAndThereAreNodeFailuresRequiresReAssignments() throws Exception {
        Set<StepExecution> stepExecutions = new HashSet<>();
        StepExecution stepExecution = mock(StepExecution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        stepExecutions.add(stepExecution);
        doReturn(executionContext).when(stepExecution).getExecutionContext();
        doReturn("Test-Node-123").when(executionContext).getString(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER);
        doReturn(stepExecutions).when(stepSplitter).split(any(), anyInt());
        doReturn(1,  0).when(databaseBackedClusterService).getPendingTasksCount(anyLong());
        doReturn(true).when(batchClusterProperties).isTracingEnabled();
        doReturn(100L).when(batchClusterProperties).getMasterTaskStatusCheckInterval();
        List<PartitionAssignmentTask> orphanedTasks = new ArrayList<>();
        orphanedTasks.add(mock(PartitionAssignmentTask.class));
        orphanedTasks.add(mock(PartitionAssignmentTask.class));
        doReturn(orphanedTasks, Collections.emptyList()).when(databaseBackedClusterService).checkForOrphanedTasks(anyLong());
        doReturn(List.of(mock(ClusterNode.class))).when(databaseBackedClusterService).getActiveNodes();
        doReturn(new int[]{1}).when(databaseBackedClusterService).updateBatchPartitionsToReAssignedNodes(any());
        Collection<StepExecution> stepExecutionCollections = clusterAwarePartitionHandler.handle(stepSplitter, managerStepExecution);
        await().atMost(5, TimeUnit.SECONDS).until(()->{
            int[] result = databaseBackedClusterService.updateBatchPartitionsToReAssignedNodes(any());
            return result != null;
        });
        assertEquals(1, stepExecutionCollections.size());
        verify(databaseBackedClusterService, times(1)).saveBatchJobCoordinationInfo(anyLong(), anyLong(), any());
        verify(databaseBackedClusterService, times(1)).saveBatchPartitions(any());
        verify(databaseBackedClusterService, times(2)).updateBatchJobCoordinationStatus(anyLong(), anyLong(), anyString());
        verify(databaseBackedClusterService, times(2)).updateBatchPartitionsToReAssignedNodes(any());
    }

    @Test
    public void testHandleWhenWaitForExecutionOfAllTasksThrowsException() throws Exception {
        Set<StepExecution> stepExecutions = new HashSet<>();
        StepExecution stepExecution = mock(StepExecution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        stepExecutions.add(stepExecution);
        doReturn(executionContext).when(stepExecution).getExecutionContext();
        doReturn("Test-Node-123").when(executionContext).getString(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER);
        doReturn(stepExecutions).when(stepSplitter).split(any(), anyInt());
        doThrow(RuntimeException.class).when(databaseBackedClusterService).getPendingTasksCount(anyLong());
        Exception exception = Assertions.assertThrows(RuntimeException.class, ()->clusterAwarePartitionHandler.handle(stepSplitter, managerStepExecution));
        assertEquals("org.springframework.batch.core.JobExecutionException: Exception occurred while waiting for workload partitions executions", exception.getMessage());
        verify(databaseBackedClusterService, times(1)).saveBatchJobCoordinationInfo(anyLong(), anyLong(), any());
        verify(databaseBackedClusterService, times(1)).saveBatchPartitions(any());
        verify(databaseBackedClusterService, times(1)).updateBatchJobCoordinationStatus(anyLong(), anyLong(), anyString());
    }

    @Test
    public void testHandleWhenMonitoringForOrphanTasksThrowsException() throws Exception {
        Set<StepExecution> stepExecutions = new HashSet<>();
        StepExecution stepExecution = mock(StepExecution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        stepExecutions.add(stepExecution);
        doReturn(executionContext).when(stepExecution).getExecutionContext();
        doReturn("Test-Node-123").when(executionContext).getString(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER);
        doReturn(stepExecutions).when(stepSplitter).split(any(), anyInt());
        doReturn(1, 0).when(databaseBackedClusterService).getPendingTasksCount(anyLong());
        doReturn(true).when(batchClusterProperties).isTracingEnabled();
        doReturn(100L).when(batchClusterProperties).getMasterTaskStatusCheckInterval();
        doThrow(RuntimeException.class).when(databaseBackedClusterService).checkForOrphanedTasks(anyLong());
        doReturn(List.of(mock(ClusterNode.class))).when(databaseBackedClusterService).getActiveNodes();

        CountDownLatch orphanTasksMonitorStarted = new CountDownLatch(1);
        CountDownLatch monitorTasksProceed = new CountDownLatch(1);

        try (MockedStatic<CompletableFuture> mockedFuture = mockStatic(CompletableFuture.class)) {
            mockedFuture.when(() -> CompletableFuture.runAsync(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable monitorForTasksCompletionTask = invocation.getArgument(0);
                        return CompletableFuture.runAsync(()-> {
                            await().atMost(5, TimeUnit.SECONDS).until(()->orphanTasksMonitorStarted.getCount() ==0);
                                try {
                                    orphanTasksMonitorStarted.await();
                                    monitorForTasksCompletionTask.run();
                                    monitorTasksProceed.countDown();
                                } catch (Exception e) {
                                    orphanTasksMonitorStarted.countDown();
                                    Thread.currentThread().interrupt();
                                }
                            }
                        );
                    }).thenAnswer(invocation -> {
                        Runnable orphanedTasksMonitorTask = invocation.getArgument(0);
                        return CompletableFuture.runAsync(() -> {
                            try {
                                orphanedTasksMonitorTask.run();
                                orphanTasksMonitorStarted.countDown();
                            } catch (Exception e) {
                                orphanTasksMonitorStarted.countDown();
                                Thread.currentThread().interrupt();
                            }
                        });
                    });
        }

        final AtomicReference<Exception> exceptionCaught = new AtomicReference<>();
        Thread testRunThread = new Thread(() -> {
            Exception exception = Assertions.assertThrows(RuntimeException.class, ()->clusterAwarePartitionHandler.handle(stepSplitter, managerStepExecution));
            exceptionCaught.set(exception);
        });
        testRunThread.start();

        await().atMost(10, TimeUnit.SECONDS).until(() -> Objects.nonNull(exceptionCaught.get()));
        testRunThread.join(2000);
        assertNotNull(exceptionCaught.get());
        assertEquals("Exception occurred while monitoring for orphaned tasks and re-arrange them to different available nodes", exceptionCaught.get().getMessage());
        verify(databaseBackedClusterService, times(1)).saveBatchJobCoordinationInfo(anyLong(), anyLong(), any());
        verify(databaseBackedClusterService, times(1)).saveBatchPartitions(any());
        verify(databaseBackedClusterService, times(1)).updateBatchJobCoordinationStatus(anyLong(), anyLong(), anyString());
        verify(databaseBackedClusterService, times(0)).updateBatchPartitionsToReAssignedNodes(any());
    }
}
