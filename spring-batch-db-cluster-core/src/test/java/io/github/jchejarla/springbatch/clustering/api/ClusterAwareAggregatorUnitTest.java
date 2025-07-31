package io.github.jchejarla.springbatch.clustering.api;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class ClusterAwareAggregatorUnitTest extends BaseUnitTest {

    @Mock
    JobExecution jobExecution;
    @Mock
    JobExplorer jobExplorer;
    @Mock
    ClusterAwareAggregatorCallback aggregatorCallback;
    ClusterAwareAggregator clusterAwareAggregator;

    @BeforeEach
    public void init() {
        clusterAwareAggregator = spy(new ClusterAwareAggregator(aggregatorCallback));
        clusterAwareAggregator.setJobExplorer(jobExplorer);
    }

    @Test
    public void testDoHandleSuccess() {
        List<StepExecution> stepExecutions = new ArrayList<>();
        StepExecution stepExecutionResult = new StepExecution("masterStep", jobExecution);
        stepExecutionResult.setStatus(BatchStatus.COMPLETED);
        doReturn(jobExecution).when(jobExplorer).getJobExecution(anyLong());
        clusterAwareAggregator.aggregate(stepExecutionResult, stepExecutions);
        verify(aggregatorCallback, times(1)).onSuccess(anyCollection());
    }

    @Test
    public void testDoHandleFailed() {
        List<StepExecution> stepExecutions = new ArrayList<>();
        StepExecution stepExecutionResult = new StepExecution("masterStep", jobExecution);
        stepExecutionResult.setStatus(BatchStatus.FAILED);
        doReturn(jobExecution).when(jobExplorer).getJobExecution(anyLong());
        clusterAwareAggregator.aggregate(stepExecutionResult, stepExecutions);
        verify(aggregatorCallback, times(1)).onFailure(anyCollection());
    }
}
