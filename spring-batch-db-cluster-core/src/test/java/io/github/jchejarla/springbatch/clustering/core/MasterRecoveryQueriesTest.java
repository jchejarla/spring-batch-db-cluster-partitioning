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
package io.github.jchejarla.springbatch.clustering.core;

import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.H2DatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.mgmt.OrphanedMasterJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the master-recovery queries against a real (in-memory H2) database, using the bundled
 * Spring Batch and cluster schema scripts. Runs without the live cluster schedulers, so it is
 * deterministic. Covers detection, the once-only atomic claim, and re-detection of an interrupted
 * recovery (a {@code RECOVERING} row whose reaper itself was lost).
 */
public class MasterRecoveryQueriesTest {

    private JdbcTemplate jdbcTemplate;
    private DatabaseBackedClusterService service;
    private long seq = 1;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:recovery-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        new ResourceDatabasePopulator(
                new ClassPathResource("org/springframework/batch/core/schema-h2.sql"),
                new ClassPathResource("schema/schema-h2.sql")
        ).execute(dataSource);

        jdbcTemplate = new JdbcTemplate(dataSource);
        service = new DatabaseBackedClusterService(jdbcTemplate, new BatchClusterProperties(), new H2DatabaseQueryProvider());

        jdbcTemplate.update("insert into batch_nodes(node_id, created_time, last_updated_time, status, host_identifier, current_load) values (?,?,?,?,?,?)",
                "alive-node", now(), now(), "ACTIVE", "host", 0);
    }

    @Test
    void detectsAndClaimsJobWhoseMasterIsGone() {
        long jobId = newJobExecution();
        insertCoordination(jobId, "ghost-master", "STARTED");

        List<OrphanedMasterJob> orphans = service.findOrphanedMasterJobs();
        assertTrue(orphans.stream().anyMatch(o -> o.jobExecutionId() == jobId && o.masterNodeId().equals("ghost-master")));

        assertTrue(service.claimOrphanedMasterJob(jobId, "ghost-master", "reaper-node", "RECOVERING"));
        // a second reaper loses: ownership has already moved off the dead master
        assertFalse(service.claimOrphanedMasterJob(jobId, "ghost-master", "reaper-node", "RECOVERING"));

        assertEquals("RECOVERING", coordinationField(jobId, "status"));
        assertEquals("reaper-node", coordinationField(jobId, "master_node_id"));
    }

    @Test
    void reDetectsInterruptedRecovery() {
        long jobId = newJobExecution();
        // a recovery a now-departed reaper claimed but never finished
        insertCoordination(jobId, "dead-reaper", "RECOVERING");
        assertTrue(service.findOrphanedMasterJobs().stream().anyMatch(o -> o.jobExecutionId() == jobId));
    }

    @Test
    void ignoresJobWhoseMasterIsStillAlive() {
        long jobId = newJobExecution();
        insertCoordination(jobId, "alive-node", "STARTED");
        assertTrue(service.findOrphanedMasterJobs().stream().noneMatch(o -> o.jobExecutionId() == jobId));
    }

    private long newJobExecution() {
        long id = seq++;
        jdbcTemplate.update("insert into batch_job_instance(job_instance_id, version, job_name, job_key) values (?,?,?,?)",
                id, 0L, "job", UUID.randomUUID().toString().replace("-", ""));
        jdbcTemplate.update("insert into batch_job_execution(job_execution_id, version, job_instance_id, create_time, status) values (?,?,?,?,?)",
                id, 0L, id, now(), "STARTED");
        return id;
    }

    private void insertCoordination(long jobExecutionId, String masterNode, String status) {
        jdbcTemplate.update("insert into batch_job_coordination(job_execution_id, master_node_id, master_step_execution_id, master_step_name, status, created_time, last_updated) values (?,?,?,?,?,?,?)",
                jobExecutionId, masterNode, jobExecutionId, "step.manager", status, now(), now());
    }

    private String coordinationField(long jobExecutionId, String column) {
        return jdbcTemplate.queryForObject("select " + column + " from batch_job_coordination where job_execution_id=?", String.class, jobExecutionId);
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}
