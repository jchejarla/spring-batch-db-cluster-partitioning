package io.github.jchejarla.springbatch.clustering.api;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class ClusterAwareAggregatorUnitTest extends BaseUnitTest {

    @Mock
    JobExecution jobExecution;
    @Mock
    ClusterAwareAggregatorCallback aggregatorCallback;
    @Mock
    JobRepository jobRepository;
    ClusterAwareAggregator clusterAwareAggregator;

    @BeforeEach
    public void init() {
        Mockito.doReturn(jobExecution).when(jobRepository).getJobExecution(anyLong());
        clusterAwareAggregator = spy(new ClusterAwareAggregator(jobRepository, aggregatorCallback));
    }

    @Test
    public void testDoHandleSuccess() {
        List<StepExecution> stepExecutions = new ArrayList<>();
        StepExecution stepExecutionResult = new StepExecution("masterStep", jobExecution);
        stepExecutionResult.setStatus(BatchStatus.COMPLETED);
        clusterAwareAggregator.aggregate(stepExecutionResult, stepExecutions);
        verify(aggregatorCallback, times(1)).onSuccess(anyCollection());
    }

    @Test
    public void testDoHandleFailed() {
        List<StepExecution> stepExecutions = new ArrayList<>();
        StepExecution stepExecutionResult = new StepExecution("masterStep", jobExecution);
        stepExecutionResult.setStatus(BatchStatus.FAILED);
        clusterAwareAggregator.aggregate(stepExecutionResult, stepExecutions);
        verify(aggregatorCallback, times(1)).onFailure(anyCollection());
    }
}
