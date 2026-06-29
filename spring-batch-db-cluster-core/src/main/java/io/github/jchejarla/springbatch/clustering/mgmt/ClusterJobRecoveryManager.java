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

import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import io.github.jchejarla.springbatch.clustering.core.CoordinationStatus;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Recovers jobs whose master node has been lost from the cluster.
 *
 * <p>In this framework the node that launches a job is that job's master: it splits the work,
 * monitors partition completion, reassigns orphaned partitions, and finalizes the job. Because
 * mastership is per-job-execution, losing a master only affects that one job &mdash; the rest of
 * the cluster is unaffected. However, a job whose master JVM dies mid-run would otherwise hang
 * forever: its Spring Batch {@code JobExecution} stays {@code STARTED} with no living process to
 * complete or fail it.</p>
 *
 * <p>This manager closes that gap. Every node periodically scans for coordination rows that are
 * still {@code STARTED} but whose master node has left the cluster (see
 * {@link DatabaseBackedClusterService#findOrphanedMasterJobs()}). For each such job it
 * <em>atomically claims</em> the row &mdash; so exactly one surviving node acts &mdash; then marks the
 * stranded {@code JobExecution} (and any still-running step executions) {@code FAILED}. A {@code FAILED}
 * execution is <strong>restartable</strong>, so the job can simply be re-submitted and resumed via
 * standard Spring Batch restart semantics, rather than remaining a permanent zombie.</p>
 *
 * <p>This is the detection-and-reaping phase of master failover. It does not (yet) automatically
 * resume the job on another node; it makes recovery a clean, operator- or client-driven restart.
 * The scan cadence is controlled by the {@code spring.batch.cluster.orphaned-master-scan-interval}
 * property (see {@link BatchClusterProperties}).</p>
 *
 * @author Janardhan Chejarla
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnClusterEnabled
public class ClusterJobRecoveryManager {

    private final DatabaseBackedClusterService databaseBackedClusterService;
    private final BatchClusterProperties batchClusterProperties;
    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;
    private final TaskScheduler clusterRecoveryScheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        clusterRecoveryScheduler.scheduleAtFixedRate(this::reapOrphanedMasterJobs,
                Duration.ofMillis(batchClusterProperties.getOrphanedMasterScanInterval()));
        log.info("Started cluster job recovery scan (interval {} ms)", batchClusterProperties.getOrphanedMasterScanInterval());
    }

    /**
     * Scans for jobs whose master node has left the cluster and abandons each one (once), so the
     * stranded execution stops hanging and becomes restartable.
     */
    protected void reapOrphanedMasterJobs() {
        try {
            List<OrphanedMasterJob> orphanedJobs = databaseBackedClusterService.findOrphanedMasterJobs();
            for (OrphanedMasterJob orphanedJob : orphanedJobs) {
                boolean claimed = databaseBackedClusterService.claimOrphanedMasterJob(
                        orphanedJob.jobExecutionId(), orphanedJob.masterNodeId(),
                        batchClusterProperties.getNodeId(), CoordinationStatus.RECOVERING.name());
                if (!claimed) {
                    // Another surviving node won the claim and is handling this job.
                    continue;
                }
                log.warn("Owner node '{}' for jobExecutionId={} has left the cluster; abandoning the stranded job execution so it can be cleanly restarted",
                        orphanedJob.masterNodeId(), orphanedJob.jobExecutionId());
                failStrandedJobExecution(orphanedJob);
                databaseBackedClusterService.updateBatchJobCoordinationStatus(
                        orphanedJob.jobExecutionId(), orphanedJob.masterStepExecutionId(), CoordinationStatus.ABANDONED.name());
            }
        } catch (Exception e) {
            log.error("Error while scanning for orphaned master jobs", e);
        }
    }

    private void failStrandedJobExecution(OrphanedMasterJob orphanedJob) {
        JobExecution jobExecution = jobExplorer.getJobExecution(orphanedJob.jobExecutionId());
        if (jobExecution == null) {
            log.warn("No JobExecution found for jobExecutionId={}; nothing to abandon", orphanedJob.jobExecutionId());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String reason = "Abandoned by cluster recovery: master node '" + orphanedJob.masterNodeId() + "' was lost";
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (isNonTerminal(stepExecution.getStatus())) {
                stepExecution.setStatus(BatchStatus.FAILED);
                stepExecution.setExitStatus(new ExitStatus(ExitStatus.FAILED.getExitCode(), reason));
                if (stepExecution.getEndTime() == null) {
                    stepExecution.setEndTime(now);
                }
                jobRepository.update(stepExecution);
            }
        }
        if (isNonTerminal(jobExecution.getStatus())) {
            jobExecution.setStatus(BatchStatus.FAILED);
            jobExecution.setExitStatus(new ExitStatus(ExitStatus.FAILED.getExitCode(), reason + "; the execution is now restartable"));
            if (jobExecution.getEndTime() == null) {
                jobExecution.setEndTime(now);
            }
            jobRepository.update(jobExecution);
        }
    }

    private boolean isNonTerminal(BatchStatus status) {
        return status == BatchStatus.STARTED
                || status == BatchStatus.STARTING
                || status == BatchStatus.STOPPING
                || status == BatchStatus.UNKNOWN;
    }

}
