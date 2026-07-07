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
package io.github.jchejarla.springbatch.clustering.polling;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeInfo;
import io.github.jchejarla.springbatch.clustering.mgmt.NodeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class PartitionedWorkerNodeTasksRunnerUnitTest extends BaseUnitTest {


    @Mock
    ApplicationContext applicationContext;
    @Mock
    JobExplorer jobExplorer;
    @Mock
    JobRepository jobRepository;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    AsyncTaskExecutor taskExecutor;
    @Mock
    BatchClusterProperties batchClusterProperties;
    @Mock
    DatabaseBackedClusterService databaseBackedClusterService;
    @Mock
    TaskScheduler partitionPollingScheduler;
    @Mock
    TaskScheduler completedTasksCleanupScheduler;
    @Mock
    TaskScheduler updateBatchPartitionsScheduler;
    @Mock
    ClusterNodeInfo clusterNodeInfo;

    PartitionedWorkerNodeTasksRunner partitionedWorkerNodeTasksRunner;

    @BeforeEach
    public void init() {
        partitionedWorkerNodeTasksRunner = spy(new PartitionedWorkerNodeTasksRunner(applicationContext,
                jobExplorer, jobRepository,taskExecutor,
                batchClusterProperties, databaseBackedClusterService,
                partitionPollingScheduler, completedTasksCleanupScheduler,
                updateBatchPartitionsScheduler, clusterNodeInfo));
        doReturn(NodeStatus.ACTIVE).when(clusterNodeInfo).getNodeStatus();
    }

    @Test
    public void testStartMonitoring() {
        partitionedWorkerNodeTasksRunner.start();
        verify(partitionPollingScheduler, times(1)).scheduleAtFixedRate(any(), any(Duration.class));
    }

    @Test
    public void testPollAndExecute() {
        List<PartitionAssignmentTask> partitionsToRun = new ArrayList<>();
        partitionsToRun.add(mock(PartitionAssignmentTask.class));
        partitionsToRun.add(mock(PartitionAssignmentTask.class));
        doReturn(partitionsToRun).when(databaseBackedClusterService).fetchPartitionAssignedTasks();
        partitionedWorkerNodeTasksRunner.pollAndExecute();
        verify(taskExecutor, times(2)).execute(any());
    }

    @Test
    public void testExecuteStep() {
        Step step = mock(Step.class);
        StepExecution stepExecution = new StepExecution("Test-Step", mock(JobExecution.class));
        PartitionAssignmentTask partitionAssignmentTask = mock(PartitionAssignmentTask.class);
        doReturn(stepExecution).when(jobExplorer).getStepExecution(anyLong(), anyLong());
        doReturn(step).when(applicationContext).getBean(any(), any(Class.class));
        partitionedWorkerNodeTasksRunner.executeStep(partitionAssignmentTask);
        verify(jobRepository, times(2)).update(any(StepExecution.class));
        assertEquals(BatchStatus.STARTED, stepExecution.getStatus());
    }

    @Test
    public void testExecuteStepWhenExecutionFails() throws Exception{
        Step step = mock(Step.class);
        StepExecution stepExecution = new StepExecution("Test-Step", mock(JobExecution.class));
        PartitionAssignmentTask partitionAssignmentTask = mock(PartitionAssignmentTask.class);
        doReturn(stepExecution).when(jobExplorer).getStepExecution(anyLong(), anyLong());
        doReturn(step).when(applicationContext).getBean(any(), any(Class.class));
        doThrow(RuntimeException.class).when(step).execute(any());
        partitionedWorkerNodeTasksRunner.executeStep(partitionAssignmentTask);
        verify(jobRepository, times(2)).update(any(StepExecution.class));
        assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
    }

    @Test
    public void testExecuteStepDoesNotFailPartitionWhenNodeIsFencedMidExecution() throws Exception {
        Step step = mock(Step.class);
        StepExecution stepExecution = new StepExecution("Test-Step", mock(JobExecution.class));
        PartitionAssignmentTask partitionAssignmentTask = mock(PartitionAssignmentTask.class);
        doReturn(stepExecution).when(jobExplorer).getStepExecution(anyLong(), anyLong());
        doReturn(step).when(applicationContext).getBean(any(), any(Class.class));
        doThrow(RuntimeException.class).when(step).execute(any());
        // ACTIVE when execution starts, then UNREACHABLE (fenced) when the step throws
        doReturn(NodeStatus.ACTIVE, NodeStatus.UNREACHABLE).when(clusterNodeInfo).getNodeStatus();

        partitionedWorkerNodeTasksRunner.executeStep(partitionAssignmentTask);

        // a fenced node must NOT finalize the partition (leave it CLAIMED for master recovery/reassignment)
        verify(databaseBackedClusterService, never()).updatePartitionStatus(any(), eq("FAILED"));
    }

    @Test
    public void testExecuteStepWhenErrorOccurredBeforeLaunchingExecution() throws JobInterruptedException {
        Step step = mock(Step.class);
        StepExecution stepExecution = new StepExecution("Test-Step", mock(JobExecution.class));
        PartitionAssignmentTask partitionAssignmentTask = mock(PartitionAssignmentTask.class);
        doReturn(stepExecution).when(jobExplorer).getStepExecution(anyLong(), anyLong());
        doReturn(step).when(applicationContext).getBean(any(), any(Class.class));
        doThrow(RuntimeException.class).when(step).execute(any());
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        partitionedWorkerNodeTasksRunner.executeStep(partitionAssignmentTask);
        verify(jobRepository, times(2)).update(any(StepExecution.class));
        verify(databaseBackedClusterService, times(1)).updatePartitionStatus(any(), argumentCaptor.capture());
        assertEquals("FAILED",argumentCaptor.getValue());
    }
}
