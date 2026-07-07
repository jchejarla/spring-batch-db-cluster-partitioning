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
package io.github.jchejarla.springbatch.clustering.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Optionally creates the cluster coordination tables ({@code BATCH_NODES},
 * {@code BATCH_JOB_COORDINATION}, {@code BATCH_PARTITIONS}, {@code BATCH_JOB_PHASE_EVENTS}) on startup,
 * mirroring Spring Batch's own schema initialization.
 *
 * <p>Driven by {@code spring.batch.cluster.initialize-schema} ({@code ALWAYS} / {@code EMBEDDED} /
 * {@code NEVER}; default {@code EMBEDDED}, i.e. only for embedded databases such as H2). The registering
 * bean is annotated {@code @DependsOnDatabaseInitialization} so this runs after the Spring Batch schema
 * has been created (by Flyway, Liquibase, or {@code spring.sql.init} — Spring Boot 4 no longer creates it
 * automatically), letting the foreign keys to {@code BATCH_JOB_EXECUTION} / {@code BATCH_STEP_EXECUTION}
 * resolve. This is intentionally a plain initializer, not a {@code DataSourceScriptDatabaseInitializer},
 * so it is not itself treated as a database initializer (which would make the ordering annotation
 * self-referential). For production, prefer a managed migration tool or apply the bundled DDL manually.</p>
 *
 * @author Janardhan Chejarla
 */
@Slf4j
public class BatchClusterDataSourceScriptDatabaseInitializer implements InitializingBean {

    private final DataSource dataSource;
    private final DatabaseInitializationMode mode;
    private final String schemaLocation;

    public BatchClusterDataSourceScriptDatabaseInitializer(DataSource dataSource, DatabaseInitializationMode mode, String schemaLocation) {
        this.dataSource = dataSource;
        this.mode = mode;
        this.schemaLocation = schemaLocation;
    }

    @Override
    public void afterPropertiesSet() {
        if (!isEnabled()) {
            return;
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new DefaultResourceLoader().getResource(schemaLocation));
        // In a cluster the nodes share one database, so several may try to create the tables on
        // startup; tolerate "already exists" from a peer rather than failing the node.
        populator.setContinueOnError(true);
        DatabasePopulatorUtils.execute(populator, dataSource);
        log.info("Cluster schema initialization applied from {}", schemaLocation);
    }

    private boolean isEnabled() {
        if (mode == DatabaseInitializationMode.NEVER || schemaLocation == null) {
            return false;
        }
        if (mode == DatabaseInitializationMode.EMBEDDED && !EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
            return false;
        }
        return true;
    }
}
