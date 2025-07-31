package io.github.jchejarla.springbatch.clustering.autoconfigure;

import io.github.jchejarla.springbatch.clustering.actuate.BatchClusterNodeHealthIndicator;
import io.github.jchejarla.springbatch.clustering.actuate.BatchClusteringInfoContributor;
import io.github.jchejarla.springbatch.clustering.core.DBSpecificQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.H2DatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.MySQLDatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.OracleDatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.PostgreSQLDatabaseQueryProvider;
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
    public DatabaseBackedClusterService batchDatabaseClusterService(JdbcTemplate jdbcTemplate, BatchClusterProperties batchClusterProperties, DBSpecificQueryProvider dbSpecificQueryProvider) {
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
            case MYSQL -> {
                return new MySQLDatabaseQueryProvider();
            }
            case ORACLE ->  {
                return new OracleDatabaseQueryProvider();
            }
            case POSTGRESQL -> {
                return new PostgreSQLDatabaseQueryProvider();
            }
            case H2 -> {
                return new H2DatabaseQueryProvider();
            }
            default -> throw new BatchConfigurationException("Unsupported Database Type " +driver.name());
        }
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
        clusterMonitoringScheduler.setPoolSize(3);
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
    public BatchClusteringInfoContributor batchClusteringInfoContributor(BatchClusterProperties batchClusterProperties) {
        return new BatchClusteringInfoContributor(batchClusterProperties);
    }

}