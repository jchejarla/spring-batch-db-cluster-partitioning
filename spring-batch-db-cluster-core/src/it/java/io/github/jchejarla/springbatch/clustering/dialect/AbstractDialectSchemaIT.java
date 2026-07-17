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
package io.github.jchejarla.springbatch.clustering.dialect;

import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.core.CoordinationStatus;
import io.github.jchejarla.springbatch.clustering.core.DBSpecificQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNode;
import io.github.jchejarla.springbatch.clustering.mgmt.OrphanedMasterJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applies the cluster DDL to a real database engine (via Testcontainers) and exercises the
 * dialect-specific query surface end-to-end. This is what closes the risk that a per-database
 * {@code schema-*.sql} or {@link DBSpecificQueryProvider} query only ever ran against H2.
 *
 * <p>The Spring Batch core schema is applied first because the cluster coordination and partition
 * tables carry foreign keys to it. Each test starts from a freshly recreated schema, so the shared
 * container stays isolated between tests.</p>
 *
 * <p>These tests pull sizeable database images and are opt-in — see each concrete subclass for the
 * system property that enables it. When Docker is unavailable they are skipped, not failed.</p>
 */
abstract class AbstractDialectSchemaIT {

    private JdbcTemplate jdbc;
    private DatabaseBackedClusterService service;
    private DBSpecificQueryProvider provider;

    /** The running database container for this dialect. */
    abstract JdbcDatabaseContainer<?> container();

    /** The query provider under test for this dialect. */
    abstract DBSpecificQueryProvider queryProvider();

    /** The dialect token used in the {@code schema[-drop]-<dialect>.sql} resource names. */
    abstract String dialect();

    private String springBatchSchema() {
        return "org/springframework/batch/core/schema-" + dialect() + ".sql";
    }

    private String springBatchDropSchema() {
        return "org/springframework/batch/core/schema-drop-" + dialect() + ".sql";
    }

    private String clusterSchema() {
        return "schema/schema-" + dialect() + ".sql";
    }

    private String clusterDropSchema() {
        return "schema/schema-drop-" + dialect() + ".sql";
    }

    @BeforeEach
    void applyFreshSchema() {
        JdbcDatabaseContainer<?> db = container();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                db.getJdbcUrl(), db.getUsername(), db.getPassword());
        dataSource.setDriverClassName(db.getDriverClassName());

        // Drop the cluster tables (FK dependents) first, then Spring Batch's; tolerate a first run
        // where nothing exists yet.
        ResourceDatabasePopulator drop = new ResourceDatabasePopulator(
                new ClassPathResource(clusterDropSchema()),
                new ClassPathResource(springBatchDropSchema()));
        drop.setContinueOnError(true);
        drop.execute(dataSource);

        // Recreate. Fails here if any CREATE TABLE / CHECK / FK / index in the dialect DDL is invalid.
        new ResourceDatabasePopulator(
                new ClassPathResource(springBatchSchema()),
                new ClassPathResource(clusterSchema())
        ).execute(dataSource);

        jdbc = new JdbcTemplate(dataSource);
        provider = queryProvider();
        service = new DatabaseBackedClusterService(jdbc, new BatchClusterProperties(), provider);
    }

    @Test
    void clusterSchemaAppliesAndDialectQueriesRunOnRealEngine() {
        // Register a node, then read it back through the active-nodes query.
        // created_time/last_updated_time are written by the DB clock (CURRENT_TIMESTAMP), so only
        // node id, status, and host are bound.
        int registered = jdbc.update(provider.getInsertQueryToRegisterNodeQuery(),
                "node-A", "ACTIVE", "host-A");
        assertEquals(1, registered);

        // last_updated_time is written by the DB clock; bind status, load, node id.
        int heartbeat = jdbc.update(provider.getUpdateNodeHeartBeatQuery(),
                "ACTIVE", 3L, "node-A");
        assertEquals(1, heartbeat);

        List<ClusterNode> active = jdbc.query(provider.getActiveNodesQuery(),
                (rs, n) -> new ClusterNode(rs.getString("node_id"), rs.getLong("current_load")));
        assertEquals(1, active.size());
        assertEquals("node-A", active.get(0).nodeId());
        assertEquals(3L, active.get(0).currentLoad());

        // Dialect-specific staleness arithmetic (TIMESTAMPDIFF / DATEDIFF / ...) must run on this engine
        // and treat a just-registered node as healthy. The threshold is deliberately enormous (30 days) so
        // the assertion is robust to any clock/timezone skew between this JVM and the database session.
        long thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000;
        int marked = jdbc.update(provider.getMarkNodesUnreachableQuery(), "UNREACHABLE", "ACTIVE", thirtyDaysMillis);
        assertEquals(0, marked, "a freshly registered node must not be marked unreachable");

        // Append-only phase event, timestamped by the database clock.
        jdbc.update(provider.getRecordPhaseEventQuery(), 1L, "node-A", "RECEIVED");
        Integer events = jdbc.queryForObject(
                "select count(*) from batch_job_phase_events where event_time is not null and node_id = ?",
                Integer.class, "node-A");
        assertEquals(1, events);
    }

    @Test
    void masterFailoverRecoveryRunsOnRealEngine() {
        // A surviving node, and a job whose master node has left the cluster.
        jdbc.update(provider.getInsertQueryToRegisterNodeQuery(), "survivor", "ACTIVE", "host");
        long jobId = 1001L;
        insertJobExecution(jobId);
        insertCoordination(jobId, "ghost-master", CoordinationStatus.STARTED.name());

        // The reaper detects the stranded job: its master_node_id is no longer in batch_nodes.
        List<OrphanedMasterJob> orphans = service.findOrphanedMasterJobs();
        assertTrue(orphans.stream().anyMatch(o -> o.jobExecutionId() == jobId && o.masterNodeId().equals("ghost-master")),
                "a job whose master left the cluster must be detected as orphaned");

        // The surviving node claims it, atomically taking ownership...
        assertTrue(service.claimOrphanedMasterJob(jobId, "ghost-master", "survivor", CoordinationStatus.RECOVERING.name()),
                "the surviving node must win the claim exactly once");
        // ...and a second attempt against the now-departed master finds nothing to claim.
        assertFalse(service.claimOrphanedMasterJob(jobId, "ghost-master", "survivor", CoordinationStatus.RECOVERING.name()),
                "the stranded job must be reaped only once");

        assertEquals("survivor", coordinationField(jobId, "master_node_id"));
        assertEquals(CoordinationStatus.RECOVERING.name(), coordinationField(jobId, "status"));
    }

    private void insertJobExecution(long jobId) {
        // Spring Batch creates its own tables in uppercase; reference them in uppercase so this works on
        // case-sensitive engines (MySQL/MariaDB) as well as the case-folding ones.
        jdbc.update("insert into BATCH_JOB_INSTANCE(job_instance_id, version, job_name, job_key) values (?,?,?,?)",
                jobId, 0L, "job", UUID.randomUUID().toString().replace("-", ""));
        jdbc.update("insert into BATCH_JOB_EXECUTION(job_execution_id, version, job_instance_id, create_time, status) values (?,?,?,?,?)",
                jobId, 0L, jobId, now(), "STARTED");
    }

    private void insertCoordination(long jobId, String masterNode, String status) {
        jdbc.update("insert into batch_job_coordination(job_execution_id, master_node_id, master_step_execution_id, master_step_name, status, created_time, last_updated) values (?,?,?,?,?,?,?)",
                jobId, masterNode, jobId, "step.manager", status, now(), now());
    }

    private String coordinationField(long jobId, String column) {
        return jdbc.queryForObject("select " + column + " from batch_job_coordination where job_execution_id=?", String.class, jobId);
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}
