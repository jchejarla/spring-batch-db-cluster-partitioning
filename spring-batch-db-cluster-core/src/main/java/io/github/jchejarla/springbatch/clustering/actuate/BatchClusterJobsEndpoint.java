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
package io.github.jchejarla.springbatch.clustering.actuate;

import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import io.github.jchejarla.springbatch.clustering.query.BatchClusterQueryService;
import io.github.jchejarla.springbatch.clustering.query.JobClusterView;
import io.github.jchejarla.springbatch.clustering.query.JobSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.List;

/**
 * Actuator endpoint exposed at {@code /actuator/batch-cluster-jobs} that lists coordinated jobs, and at
 * {@code /actuator/batch-cluster-jobs/{jobExecutionId}} the job-centric view (master node, partition count,
 * status histogram, and per-partition placement) for one job. Read-only; a thin wrapper over
 * {@link BatchClusterQueryService}.
 *
 * @author Janardhan Chejarla
 */
@RequiredArgsConstructor
@ConditionalOnClusterEnabled
@Endpoint(id = "batch-cluster-jobs")
public class BatchClusterJobsEndpoint {

    private final BatchClusterQueryService queryService;

    @ReadOperation
    public List<JobSummaryView> jobs() {
        return queryService.listCoordinatedJobs();
    }

    @ReadOperation
    public JobClusterView job(@Selector String jobExecutionId) {
        return queryService.getJobView(Long.parseLong(jobExecutionId)).orElse(null);
    }
}
