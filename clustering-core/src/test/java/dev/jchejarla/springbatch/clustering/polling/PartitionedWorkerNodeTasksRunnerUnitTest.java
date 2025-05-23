package dev.jchejarla.springbatch.clustering.polling;

import dev.jchejarla.springbatch.clustering.BaseUnitTest;
import dev.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import dev.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
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
    TaskExecutor taskExecutor;
    @Mock
    BatchClusterProperties batchClusterProperties;
    @Mock
    DatabaseBackedClusterService databaseBackedClusterService;
    @Mock
    TaskScheduler partitionPollingScheduler;

    PartitionedWorkerNodeTasksRunner partitionedWorkerNodeTasksRunner;

    @BeforeEach
    public void init() {
        partitionedWorkerNodeTasksRunner = spy(new PartitionedWorkerNodeTasksRunner(applicationContext,jobExplorer, jobRepository,taskExecutor,
                                                                                    batchClusterProperties, databaseBackedClusterService, partitionPollingScheduler));
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
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
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
    public void testExecuteStepWhenErrorOccurredBeforeLaunchingExecution() {
        Step step = mock(Step.class);
        StepExecution stepExecution = new StepExecution("Test-Step", mock(JobExecution.class));
        PartitionAssignmentTask partitionAssignmentTask = mock(PartitionAssignmentTask.class);
        doReturn(stepExecution).when(jobExplorer).getStepExecution(anyLong(), anyLong());
        doReturn(step).when(applicationContext).getBean(any(), any(Class.class));
        doThrow(RuntimeException.class).when(applicationContext).getBean(any(), any(Class.class));
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        partitionedWorkerNodeTasksRunner.executeStep(partitionAssignmentTask);
        verify(jobRepository, times(1)).update(any(StepExecution.class));
        verify(databaseBackedClusterService, times(1)).updatePartitionStatus(any(), argumentCaptor.capture());
        assertEquals("FAILED",argumentCaptor.getValue());
    }
}
