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
package io.github.jchejarla.springbatch.clustering.query;

import io.github.jchejarla.springbatch.clustering.core.DBSpecificQueryProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Read-only, job-centric view of the cluster: which node masters a job, how many partitions it has, and
 * where each partition is running. This complements the node-centric actuator endpoint.
 *
 * <p>Purely a set of {@code SELECT}s over the coordination tables — it never mutates state and is kept off
 * the hot coordination path. Applications can call it directly to build their own UI/API; the actuator
 * job endpoint is a thin wrapper over it.</p>
 *
 * @author Janardhan Chejarla
 */
@RequiredArgsConstructor
public class BatchClusterQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final DBSpecificQueryProvider queryProvider;

    /** Lists coordinated jobs, most recent first. */
    public List<JobSummaryView> listCoordinatedJobs() {
        return jdbcTemplate.query(queryProvider.getCoordinatedJobsQuery(),
                (rs, rowNum) -> new JobSummaryView(
                        rs.getLong("job_execution_id"),
                        rs.getString("master_node_id"),
                        rs.getString("master_step_name"),
                        rs.getString("status")));
    }

    /** Returns the job-centric view for one job execution, or empty if it is not (or no longer) coordinated. */
    public Optional<JobClusterView> getJobView(long jobExecutionId) {
        List<Coordination> coordination = jdbcTemplate.query(queryProvider.getJobCoordinationByIdQuery(),
                (rs, rowNum) -> new Coordination(rs.getString("master_node_id"), rs.getLong("master_step_execution_id"), rs.getString("status")),
                jobExecutionId);
        if (coordination.isEmpty()) {
            return Optional.empty();
        }
        Coordination coord = coordination.get(0);

        List<PartitionView> partitions = jdbcTemplate.query(queryProvider.getPartitionsByMasterStepQuery(),
                (rs, rowNum) -> new PartitionView(
                        rs.getLong("step_execution_id"),
                        rs.getString("partition_key"),
                        rs.getString("assigned_node"),
                        rs.getString("status")),
                coord.masterStepExecutionId());

        Map<String, Long> statusCounts = partitions.stream()
                .collect(Collectors.groupingBy(PartitionView::status, Collectors.counting()));

        return Optional.of(new JobClusterView(jobExecutionId, coord.masterNode(), coord.status(),
                partitions.size(), statusCounts, partitions));
    }

    private record Coordination(String masterNode, long masterStepExecutionId, String status) {
    }
}
