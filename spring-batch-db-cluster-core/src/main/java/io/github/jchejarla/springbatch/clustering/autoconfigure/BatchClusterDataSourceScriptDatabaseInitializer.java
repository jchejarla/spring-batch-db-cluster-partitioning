package io.github.jchejarla.springbatch.clustering.autoconfigure;

import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

import javax.sql.DataSource;
import java.util.List;

/**
 * Optionally creates the cluster coordination tables ({@code BATCH_NODES},
 * {@code BATCH_JOB_COORDINATION}, {@code BATCH_PARTITIONS}) on startup, mirroring Spring Batch's own
 * schema initialization.
 *
 * <p>Driven by {@code spring.batch.cluster.initialize-schema} ({@code ALWAYS} / {@code EMBEDDED} /
 * {@code NEVER}; default {@code EMBEDDED}, i.e. only for embedded databases such as H2). It is
 * registered to run after the Spring Batch schema initializer so the foreign keys to
 * {@code BATCH_JOB_EXECUTION} / {@code BATCH_STEP_EXECUTION} resolve. For production, prefer a managed
 * migration tool (Flyway/Liquibase) or apply the bundled DDL manually.</p>
 *
 * @author Janardhan Chejarla
 */
public class BatchClusterDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer {

    public BatchClusterDataSourceScriptDatabaseInitializer(DataSource dataSource, DatabaseInitializationMode mode, String schemaLocation) {
        super(dataSource, settings(mode, schemaLocation));
    }

    private static DatabaseInitializationSettings settings(DatabaseInitializationMode mode, String schemaLocation) {
        DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
        settings.setSchemaLocations(List.of(schemaLocation));
        settings.setMode(mode);
        // In a cluster the nodes share one database, so several may try to create the tables on
        // startup; tolerate "already exists" from a peer rather than failing the node.
        settings.setContinueOnError(true);
        return settings;
    }
}
