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

import io.github.jchejarla.springbatch.clustering.actuate.BatchClusterHealthIndicator;
import io.github.jchejarla.springbatch.clustering.actuate.BatchClusterNodeHealthIndicator;
import io.github.jchejarla.springbatch.clustering.actuate.BatchClusterNodesEndpoint;
import io.github.jchejarla.springbatch.clustering.actuate.BatchClusteringInfoContributor;
import io.github.jchejarla.springbatch.clustering.core.DBSpecificQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.DB2DatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.H2DatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.MySQLDatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.OracleDatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.PostgreSQLDatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.SQLServerDatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterJobRecoveryManager;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeInfo;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeManager;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeStatusChangeConditionNotifier;
import io.github.jchejarla.springbatch.clustering.partition.ClusterAwarePartitionHandler;
import io.github.jchejarla.springbatch.clustering.polling.PartitionedWorkerNodeTasksRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@AutoConfiguration
@ConditionalOnProperty(name = "spring.batch.cluster.enabled", havingValue = "true")
@EnableConfigurationProperties({BatchClusterProperties.class})
@AutoConfigureAfter({DataSourceAutoConfiguration.class, BatchAutoConfiguration.class})
@RequiredArgsConstructor
@EnableScheduling
public class BatchClusterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DatabaseBackedClusterService databaseBackedClusterService(JdbcTemplate jdbcTemplate, BatchClusterProperties batchClusterProperties, DBSpecificQueryProvider dbSpecificQueryProvider) {
        return new DatabaseBackedClusterService(jdbcTemplate, batchClusterProperties, dbSpecificQueryProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClusterAwarePartitionHandler clusterAwarePartitionHandler(
            DatabaseBackedClusterService databaseBackedClusterService,
            BatchClusterProperties batchClusterProperties) {
        return new ClusterAwarePartitionHandler(databaseBackedClusterService, batchClusterProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClusterNodeManager clusterNodeManager(DatabaseBackedClusterService databaseBackedClusterService,
                                                 BatchClusterProperties batchClusterProperties,
                                                 @Qualifier("clusterHealthMonitoringScheduler") TaskScheduler clusterMonitoringScheduler,
                                                 ClusterNodeInfo clusterNodeInfo,
                                                 ClusterNodeStatusChangeConditionNotifier clusterNodeStatusChangeConditionNotifier) {
        return new ClusterNodeManager(databaseBackedClusterService, batchClusterProperties, clusterMonitoringScheduler, clusterNodeInfo, clusterNodeStatusChangeConditionNotifier);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClusterJobRecoveryManager clusterJobRecoveryManager(DatabaseBackedClusterService databaseBackedClusterService,
                                                              BatchClusterProperties batchClusterProperties,
                                                              JobExplorer jobExplorer,
                                                              JobRepository jobRepository,
                                                              @Qualifier("clusterHealthMonitoringScheduler") TaskScheduler clusterMonitoringScheduler) {
        return new ClusterJobRecoveryManager(databaseBackedClusterService, batchClusterProperties, jobExplorer, jobRepository, clusterMonitoringScheduler);
    }

    @Bean
    @ConditionalOnMissingBean
    public PartitionedWorkerNodeTasksRunner partitionWorkerTasksRunner(ApplicationContext applicationContext,
                                                                       JobExplorer jobExplorer,
                                                                       JobRepository jobRepository,
                                                                       @Qualifier("clusteredStepsTasksExecutor") TaskExecutor taskExecutor,
                                                                       BatchClusterProperties batchClusterProperties,
                                                                       DatabaseBackedClusterService databaseBackedClusterService,
                                                                       @Qualifier("partitionPollingScheduler") TaskScheduler partitionPollingScheduler,
                                                                       @Qualifier("completedTasksCleanupScheduler") TaskScheduler completedTasksCleanupScheduler,
                                                                       @Qualifier("updateBatchPartitionsScheduler") TaskScheduler updateBatchPartitionsScheduler,
                                                                       ClusterNodeInfo clusterNodeInfo) {
        return new PartitionedWorkerNodeTasksRunner(applicationContext,
                                                    jobExplorer,
                                                    jobRepository,
                                                    taskExecutor,
                                                    batchClusterProperties,
                                                    databaseBackedClusterService,
                                                    partitionPollingScheduler,
                                                    completedTasksCleanupScheduler,
                                                    updateBatchPartitionsScheduler,
                                                    clusterNodeInfo);
    }

    @Bean
    @ConditionalOnMissingBean
    public DBSpecificQueryProvider dbSpecificQueryProvider(DataSource dataSource) throws SQLException {
        String url = getDatabaseURL(dataSource);
        DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(url);
        switch(driver) {
            case MYSQL, MARIADB -> {
                return new MySQLDatabaseQueryProvider();
            }
            case ORACLE ->  {
                return new OracleDatabaseQueryProvider();
            }
            case POSTGRESQL -> {
                return new PostgreSQLDatabaseQueryProvider();
            }
            case SQLSERVER -> {
                return new SQLServerDatabaseQueryProvider();
            }
            case DB2 -> {
                return new DB2DatabaseQueryProvider();
            }
            case H2 -> {
                return new H2DatabaseQueryProvider();
            }
            default -> throw new BatchConfigurationException("Unsupported Database Type " +driver.name());
        }
    }


    /**
     * Optionally creates the cluster tables on startup, controlled by
     * {@code spring.batch.cluster.initialize-schema} (see {@link BatchClusterProperties}). Runs after
     * {@code batchDataSourceInitializer} so the foreign keys to the Spring Batch tables resolve.
     */
    @Bean
    @ConditionalOnMissingBean
    @DependsOn("batchDataSourceInitializer")
    public BatchClusterDataSourceScriptDatabaseInitializer batchClusterDataSourceScriptDatabaseInitializer(
            DataSource dataSource, BatchClusterProperties batchClusterProperties) throws SQLException {
        DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(getDatabaseURL(dataSource));
        return new BatchClusterDataSourceScriptDatabaseInitializer(dataSource, batchClusterProperties.getInitializeSchema(), clusterSchemaLocation(driver));
    }

    static String clusterSchemaLocation(DatabaseDriver driver) {
        String name = switch (driver) {
            case POSTGRESQL -> "postgres";
            case MYSQL -> "mysql";
            case MARIADB -> "mariadb";
            case ORACLE -> "oracle";
            case SQLSERVER -> "sqlserver";
            case DB2 -> "db2";
            case H2 -> "h2";
            default -> throw new BatchConfigurationException("Unsupported Database Type " + driver.name());
        };
        return "classpath:schema/schema-" + name + ".sql";
    }

    private String getDatabaseURL(DataSource dataSource) throws SQLException {
        if(dataSource instanceof DriverManagerDataSource) {
            return ((DriverManagerDataSource) dataSource).getUrl();
        }
        try (Connection conn = dataSource.getConnection()) {
            return conn.getMetaData().getURL();
        }
    }

    // Different scheduler for dedicated tasks
    @Bean(name = "clusterHealthMonitoringScheduler")
    public TaskScheduler clusterMonitoringScheduler() {
        ThreadPoolTaskScheduler clusterMonitoringScheduler = new ThreadPoolTaskScheduler();
        clusterMonitoringScheduler.setPoolSize(4);
        clusterMonitoringScheduler.setThreadNamePrefix("cluster-monitoring-task-");
        clusterMonitoringScheduler.initialize();
        return clusterMonitoringScheduler;
    }

    @Bean(name = "partitionPollingScheduler")
    public TaskScheduler partitionPollingScheduler() {
        ThreadPoolTaskScheduler partitionPollingScheduler = new ThreadPoolTaskScheduler();
        partitionPollingScheduler.setPoolSize(1);
        partitionPollingScheduler.setThreadNamePrefix("partition-polling-task-");
        partitionPollingScheduler.initialize();
        return partitionPollingScheduler;
    }

    @Bean(name = "completedTasksCleanupScheduler")
    public TaskScheduler completedTasksCleanupScheduler() {
        ThreadPoolTaskScheduler completedTasksCleanupScheduler = new ThreadPoolTaskScheduler();
        completedTasksCleanupScheduler.setPoolSize(1);
        completedTasksCleanupScheduler.setThreadNamePrefix("completed-tasks-cleanup-");
        completedTasksCleanupScheduler.initialize();
        return completedTasksCleanupScheduler;
    }

    @Bean(name = "updateBatchPartitionsScheduler")
    public TaskScheduler updateBatchPartitionsScheduler() {
        ThreadPoolTaskScheduler completedTasksCleanupScheduler = new ThreadPoolTaskScheduler();
        completedTasksCleanupScheduler.setPoolSize(1);
        completedTasksCleanupScheduler.setThreadNamePrefix("update-batch-partitions-");
        completedTasksCleanupScheduler.initialize();
        return completedTasksCleanupScheduler;
    }


    @Bean("clusteredStepsTasksExecutor")
    public TaskExecutor clusteredStepsTasksExecutor(BatchClusterProperties batchClusterProperties) {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(batchClusterProperties.getConcurrencyLimitPerNode());
        taskExecutor.setThreadNamePrefix("Clustered-Task-Async-Executor");
        return taskExecutor;
    }

    @Bean
    public ClusterNodeInfo clusterNodeInfo(BatchClusterProperties batchClusterProperties) {
        return new ClusterNodeInfo(batchClusterProperties.getNodeId());
    }

    @Bean
    public BatchClusterNodeHealthIndicator batchClusterNodeHealthIndicator(ClusterNodeInfo clusterNodeInfo) {
        return new BatchClusterNodeHealthIndicator(clusterNodeInfo);
    }

    @Bean
    public BatchClusterHealthIndicator batchClusterHealthIndicator(ClusterNodeManager clusterNodeManager) {
        return new BatchClusterHealthIndicator(clusterNodeManager);
    }

    @Bean
    public BatchClusteringInfoContributor batchClusteringInfoContributor(BatchClusterProperties batchClusterProperties) {
        return new BatchClusteringInfoContributor(batchClusterProperties);
    }

    @Bean
    public BatchClusterNodesEndpoint batchClusterNodesEndpoint(ClusterNodeManager clusterNodeManager) {
        return new BatchClusterNodesEndpoint(clusterNodeManager);
    }


}