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

import io.github.jchejarla.springbatch.clustering.core.serviceimpl.H2DatabaseQueryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the job-centric read queries against a real (in-memory H2) database, using the bundled
 * Spring Batch and cluster schema scripts.
 */
public class BatchClusterQueryServiceTest {

    private JdbcTemplate jdbcTemplate;
    private BatchClusterQueryService service;
    private long seq = 1;

    @BeforeEach
    void setUp() {
        // SingleConnectionDataSource keeps one physical connection open for the test. H2 2.4.240 binds a
        // table's CHECK constraint to the creating connection; with a connection-per-op DataSource that
        // connection closes and later inserts into the CHECK-constrained batch_partitions table fail with
        // "database has been closed". Holding the connection open avoids that test-only quirk.
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
                "jdbc:h2:mem:query-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "", true);
        dataSource.setDriverClassName("org.h2.Driver");
        new ResourceDatabasePopulator(
                new ClassPathResource("org/springframework/batch/core/schema-h2.sql"),
                new ClassPathResource("schema/schema-h2.sql")
        ).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        service = new BatchClusterQueryService(jdbcTemplate, new H2DatabaseQueryProvider());
    }

    @Test
    void getJobViewReturnsPlacementAndStatusHistogram() {
        long jobId = newJobExecution();
        long masterStep = 9000L;
        insertCoordination(jobId, "master-1", masterStep, "step.manager", "STARTED");
        insertPartition(newStepExecution(jobId), jobId, "k1", "node-a", "COMPLETED", masterStep);
        insertPartition(newStepExecution(jobId), jobId, "k2", "node-b", "COMPLETED", masterStep);
        insertPartition(newStepExecution(jobId), jobId, "k3", "node-a", "CLAIMED", masterStep);

        Optional<JobClusterView> view = service.getJobView(jobId);

        assertTrue(view.isPresent());
        JobClusterView v = view.get();
        assertEquals("master-1", v.masterNode());
        assertEquals("STARTED", v.coordinationStatus());
        assertEquals(3, v.partitionCount());
        assertEquals(2L, v.statusCounts().get("COMPLETED"));
        assertEquals(1L, v.statusCounts().get("CLAIMED"));
        assertEquals(3, v.partitions().size());
    }

    @Test
    void listCoordinatedJobsIncludesTheJob() {
        long jobId = newJobExecution();
        insertCoordination(jobId, "master-1", 9001L, "step.manager", "STARTED");

        List<JobSummaryView> jobs = service.listCoordinatedJobs();

        assertTrue(jobs.stream().anyMatch(j -> j.jobExecutionId() == jobId && "master-1".equals(j.masterNode())));
    }

    @Test
    void getJobViewIsEmptyForUnknownJob() {
        assertTrue(service.getJobView(999999L).isEmpty());
    }

    private long newJobExecution() {
        long id = seq++;
        jdbcTemplate.update("insert into batch_job_instance(job_instance_id, version, job_name, job_key) values (?,?,?,?)",
                id, 0L, "job", UUID.randomUUID().toString().replace("-", ""));
        jdbcTemplate.update("insert into batch_job_execution(job_execution_id, version, job_instance_id, create_time, status) values (?,?,?,?,?)",
                id, 0L, id, now(), "STARTED");
        return id;
    }

    private long newStepExecution(long jobExecutionId) {
        long id = seq++;
        jdbcTemplate.update("insert into batch_step_execution(step_execution_id, version, step_name, job_execution_id, create_time, status) values (?,?,?,?,?,?)",
                id, 0L, "step", jobExecutionId, now(), "STARTED");
        return id;
    }

    private void insertCoordination(long jobExecutionId, String masterNode, long masterStepExecutionId, String masterStepName, String status) {
        jdbcTemplate.update("insert into batch_job_coordination(job_execution_id, master_node_id, master_step_execution_id, master_step_name, status, created_time, last_updated) values (?,?,?,?,?,?,?)",
                jobExecutionId, masterNode, masterStepExecutionId, masterStepName, status, now(), now());
    }

    private void insertPartition(long stepExecutionId, long jobExecutionId, String key, String node, String status, long masterStepExecutionId) {
        jdbcTemplate.update("insert into batch_partitions(step_execution_id, job_execution_id, partition_key, assigned_node, status, master_step_execution_id, is_transferable) values (?,?,?,?,?,?,?)",
                stepExecutionId, jobExecutionId, key, node, status, masterStepExecutionId, 1);
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}
