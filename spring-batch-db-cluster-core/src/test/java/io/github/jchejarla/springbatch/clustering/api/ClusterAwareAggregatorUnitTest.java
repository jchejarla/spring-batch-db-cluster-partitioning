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
package io.github.jchejarla.springbatch.clustering.api;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class ClusterAwareAggregatorUnitTest extends BaseUnitTest {

    @Mock
    JobRepository jobRepository;
    @Mock
    ClusterAwareAggregatorCallback aggregatorCallback;
    ClusterAwareAggregator clusterAwareAggregator;

    @BeforeEach
    public void init() {
        clusterAwareAggregator = spy(new ClusterAwareAggregator(aggregatorCallback, jobRepository));
    }

    @Test
    public void testDoHandleSuccess() {
        // A non-failed aggregate result must be dispatched to the success callback.
        aggregate(BatchStatus.COMPLETED);
        verify(aggregatorCallback, times(1)).onSuccess(anyCollection());
        verify(aggregatorCallback, never()).onFailure(anyCollection());
    }

    @Test
    public void testDoHandleFailed() {
        // A failed worker drives the aggregate result to FAILED, dispatched to the failure callback.
        aggregate(BatchStatus.FAILED);
        verify(aggregatorCallback, times(1)).onFailure(anyCollection());
        verify(aggregatorCallback, never()).onSuccess(anyCollection());
    }

    @Test
    public void testCallbackReceivesAllPartitionExecutions() {
        // Batch 6's RemoteStepExecutionAggregator reloads the master JobExecution and filters its step
        // executions to the passed IDs, so the callback receives the *reloaded* set. Guard that it is the
        // FULL partition set (not an empty/partial collection from a mismatched reload).
        JobExecution jobExecution = new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters());
        StepExecution worker1 = new StepExecution(11L, "worker1", jobExecution);
        StepExecution worker2 = new StepExecution(12L, "worker2", jobExecution);
        worker1.setStatus(BatchStatus.COMPLETED);
        worker2.setStatus(BatchStatus.COMPLETED);
        jobExecution.addStepExecution(worker1);
        jobExecution.addStepExecution(worker2);
        StepExecution masterResult = new StepExecution(1L, "masterStep", jobExecution);

        doReturn(jobExecution).when(jobRepository).getJobExecution(anyLong());
        clusterAwareAggregator.aggregate(masterResult, List.of(worker1, worker2));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<StepExecution>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(aggregatorCallback).onSuccess(captor.capture());
        assertEquals(2, captor.getValue().size(), "callback must receive all partition step executions");
        assertTrue(captor.getValue().containsAll(List.of(worker1, worker2)));
    }

    /**
     * As of Spring Batch 6, {@code RemoteStepExecutionAggregator} reloads the job execution (and its step
     * executions) from the {@link JobRepository} before delegating, so the test wires a persisted worker
     * step of the given status and lets the aggregator derive the master result status from it.
     */
    private void aggregate(BatchStatus workerStatus) {
        JobExecution jobExecution = new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters());
        StepExecution worker = new StepExecution("worker", jobExecution);
        worker.setStatus(workerStatus);
        jobExecution.addStepExecution(worker);
        StepExecution masterResult = new StepExecution("masterStep", jobExecution);

        doReturn(jobExecution).when(jobRepository).getJobExecution(anyLong());
        clusterAwareAggregator.aggregate(masterResult, List.of(worker));
    }
}
