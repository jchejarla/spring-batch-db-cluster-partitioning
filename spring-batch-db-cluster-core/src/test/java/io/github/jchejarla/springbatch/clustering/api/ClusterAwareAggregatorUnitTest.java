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
