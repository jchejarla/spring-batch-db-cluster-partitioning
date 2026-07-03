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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that phase-timing events are appended to {@code BATCH_JOB_PHASE_EVENTS} against a real
 * (in-memory H2) database, timestamped by the database clock.
 */
public class PhaseTimingCaptureTest {

    private JdbcTemplate jdbcTemplate;
    private DatabaseBackedClusterService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:phase-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        new ResourceDatabasePopulator(
                new ClassPathResource("org/springframework/batch/core/schema-h2.sql"),
                new ClassPathResource("schema/schema-h2.sql")
        ).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);

        BatchClusterProperties properties = mock(BatchClusterProperties.class);
        when(properties.getNodeId()).thenReturn("node-1");
        service = new DatabaseBackedClusterService(jdbcTemplate, properties, new H2DatabaseQueryProvider());
    }

    @Test
    void recordsPhaseEventsWithNodeAndDbClock() {
        service.recordPhaseEvent(1L, "RECEIVED");
        service.recordPhaseEvent(1L, "PARTITIONED");

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from batch_job_phase_events where job_execution_id = ?", Integer.class, 1L);
        assertEquals(2, count);

        Integer missingTimeOrNode = jdbcTemplate.queryForObject(
                "select count(*) from batch_job_phase_events where event_time is null or node_id is null", Integer.class);
        assertEquals(0, missingTimeOrNode, "every event must carry a db-clock timestamp and node id");

        String node = jdbcTemplate.queryForObject(
                "select node_id from batch_job_phase_events where phase = 'RECEIVED'", String.class);
        assertEquals("node-1", node);
    }
}
