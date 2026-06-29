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
package io.github.jchejarla.springbatch.clustering.mgmt;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.core.CoordinationStatus;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.scheduling.TaskScheduler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ClusterJobRecoveryManagerUnitTest extends BaseUnitTest {

    @Mock
    DatabaseBackedClusterService databaseBackedClusterService;
    @Mock
    BatchClusterProperties batchClusterProperties;
    @Mock
    JobExplorer jobExplorer;
    @Mock
    JobRepository jobRepository;
    @Mock
    TaskScheduler clusterRecoveryScheduler;

    ClusterJobRecoveryManager recoveryManager;

    @BeforeEach
    public void init() {
        recoveryManager = new ClusterJobRecoveryManager(databaseBackedClusterService, batchClusterProperties, jobExplorer, jobRepository, clusterRecoveryScheduler);
        doReturn("this-node").when(batchClusterProperties).getNodeId();
    }

    @Test
    public void testStartSchedulesScan() {
        doReturn(10000L).when(batchClusterProperties).getOrphanedMasterScanInterval();
        recoveryManager.start();
        verify(clusterRecoveryScheduler, times(1)).scheduleAtFixedRate(any(), any(java.time.Duration.class));
    }

    @Test
    public void testClaimedOrphanIsFailedAndAbandoned() {
        OrphanedMasterJob orphan = new OrphanedMasterJob(1L, "deadNode", 10L, "step.manager");
        doReturn(List.of(orphan)).when(databaseBackedClusterService).findOrphanedMasterJobs();
        doReturn(true).when(databaseBackedClusterService)
                .claimOrphanedMasterJob(eq(1L), eq("deadNode"), eq("this-node"), eq(CoordinationStatus.RECOVERING.name()));

        JobExecution jobExecution = new JobExecution(1L);
        jobExecution.setStatus(BatchStatus.STARTED);
        StepExecution stepExecution = jobExecution.createStepExecution("step");
        stepExecution.setStatus(BatchStatus.STARTED);
        doReturn(jobExecution).when(jobExplorer).getJobExecution(1L);

        recoveryManager.reapOrphanedMasterJobs();

        // both the in-flight step and the job are driven to a terminal, restartable FAILED state
        assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
        assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
        verify(jobRepository, times(1)).update(stepExecution);
        verify(jobRepository, times(1)).update(jobExecution);
        verify(databaseBackedClusterService, times(1)).updateBatchJobCoordinationStatus(1L, 10L, CoordinationStatus.ABANDONED.name());
    }

    @Test
    public void testLostClaimIsSkipped() {
        OrphanedMasterJob orphan = new OrphanedMasterJob(2L, "deadNode", 20L, "step.manager");
        doReturn(List.of(orphan)).when(databaseBackedClusterService).findOrphanedMasterJobs();
        doReturn(false).when(databaseBackedClusterService)
                .claimOrphanedMasterJob(eq(2L), eq("deadNode"), eq("this-node"), eq(CoordinationStatus.RECOVERING.name()));

        recoveryManager.reapOrphanedMasterJobs();

        // another node won the claim: this node must not touch the job execution or coordination row
        verifyNoInteractions(jobExplorer);
        verifyNoInteractions(jobRepository);
        verify(databaseBackedClusterService, never()).updateBatchJobCoordinationStatus(anyLong(), anyLong(), anyString());
    }

    @Test
    public void testNoOrphansDoesNothing() {
        doReturn(List.of()).when(databaseBackedClusterService).findOrphanedMasterJobs();

        recoveryManager.reapOrphanedMasterJobs();

        verify(databaseBackedClusterService, never()).claimOrphanedMasterJob(anyLong(), anyString(), anyString(), anyString());
        verifyNoInteractions(jobRepository);
    }
}
