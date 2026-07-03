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

import io.github.jchejarla.springbatch.clustering.core.DBSpecificQueryProvider;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNode;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Applies the cluster DDL to a real database engine (via Testcontainers) and exercises the
 * dialect-specific query surface end-to-end. This is what closes the risk that a per-database
 * {@code schema-*.sql} or {@link DBSpecificQueryProvider} query only ever ran against H2.
 *
 * <p>The Spring Batch core schema is applied first because the cluster coordination and partition
 * tables carry foreign keys to it. The node-registry lifecycle and the phase-event log have no
 * external keys, so they are the parts we assert against directly.</p>
 *
 * <p>These tests pull sizeable database images and are opt-in — see each concrete subclass for the
 * system property that enables it. When Docker is unavailable they are skipped, not failed.</p>
 */
abstract class AbstractDialectSchemaIT {

    /** The running database container for this dialect. */
    abstract JdbcDatabaseContainer<?> container();

    /** The query provider under test for this dialect. */
    abstract DBSpecificQueryProvider queryProvider();

    /** Classpath location of Spring Batch's own DDL for this dialect (creates the referenced tables). */
    abstract String springBatchSchema();

    /** Classpath location of the cluster DDL for this dialect. */
    abstract String clusterSchema();

    @Test
    void clusterSchemaAppliesAndDialectQueriesRunOnRealEngine() {
        JdbcDatabaseContainer<?> db = container();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                db.getJdbcUrl(), db.getUsername(), db.getPassword());
        dataSource.setDriverClassName(db.getDriverClassName());

        // Fails here if any CREATE TABLE / CHECK / FK / index in the dialect DDL is invalid for this engine.
        new ResourceDatabasePopulator(
                new ClassPathResource(springBatchSchema()),
                new ClassPathResource(clusterSchema())
        ).execute(dataSource);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        DBSpecificQueryProvider provider = queryProvider();

        // Register a node, then read it back through the active-nodes query.
        int registered = jdbc.update(provider.getInsertQueryToRegisterNodeQuery(),
                "node-A", new Date(), new Date(), "ACTIVE", "host-A");
        assertEquals(1, registered);

        int heartbeat = jdbc.update(provider.getUpdateNodeHeartBeatQuery(),
                new Date(), "ACTIVE", 3L, "node-A");
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
}
