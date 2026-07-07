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

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.core.DBSpecificQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.MySQLDatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.OracleDatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.PostgreSQLDatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeInfo;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeManager;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeStatusChangeConditionNotifier;
import io.github.jchejarla.springbatch.clustering.polling.PartitionedWorkerNodeTasksRunner;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BatchClusterAutoConfigurationUnitTest extends BaseUnitTest {

    @Mock
    JdbcTemplate jdbcTemplate;
    @Mock
    BatchClusterProperties batchClusterProperties;
    @Mock
    DBSpecificQueryProvider dbSpecificQueryProvider;

    @Spy
    BatchClusterAutoConfiguration batchClusterAutoConfiguration;

    @Test
    public void testBatchDatabaseClusterService() {
        DatabaseBackedClusterService databaseBackedClusterService = batchClusterAutoConfiguration.databaseBackedClusterService(jdbcTemplate, batchClusterProperties, dbSpecificQueryProvider);
        assertNotNull(databaseBackedClusterService);
    }

    @Test
    public void testClusterAwarePartitionHandler() {
        DatabaseBackedClusterService databaseBackedClusterService = batchClusterAutoConfiguration.databaseBackedClusterService(jdbcTemplate, batchClusterProperties, dbSpecificQueryProvider);
        batchClusterAutoConfiguration.clusterAwarePartitionHandler(databaseBackedClusterService, batchClusterProperties);
    }

    @Test
    public void testClusterNodeManager() {
        DatabaseBackedClusterService databaseBackedClusterService = batchClusterAutoConfiguration.databaseBackedClusterService(jdbcTemplate, batchClusterProperties, dbSpecificQueryProvider);
        ClusterNodeManager nodeManager = batchClusterAutoConfiguration.clusterNodeManager(databaseBackedClusterService, batchClusterProperties,
                mock(TaskScheduler.class),
                mock(ClusterNodeInfo.class),
                mock(ClusterNodeStatusChangeConditionNotifier.class));
        assertNotNull(nodeManager);
    }

    @Test
    public void testPartitionWorkerTasksRunner() {
        DatabaseBackedClusterService databaseBackedClusterService = batchClusterAutoConfiguration.databaseBackedClusterService(jdbcTemplate, batchClusterProperties, dbSpecificQueryProvider);
        PartitionedWorkerNodeTasksRunner tasksRunner = batchClusterAutoConfiguration.partitionWorkerTasksRunner(mock(ApplicationContext.class),
                mock(JobExplorer.class), mock(JobRepository.class), mock(TaskExecutor.class),
                batchClusterProperties, databaseBackedClusterService, mock(TaskScheduler.class),
                mock(TaskScheduler.class), mock(TaskScheduler.class), mock(ClusterNodeInfo.class));
        assertNotNull(tasksRunner);
    }

    @Test
    public void testDbSpecificQueryProvider() throws Exception {
        DatabaseDriver MYSQL = DatabaseDriver.MYSQL;
        DatabaseDriver ORACLE = DatabaseDriver.ORACLE;
        DatabaseDriver POSTGRESQL = DatabaseDriver.POSTGRESQL;
        DatabaseDriver H2 = DatabaseDriver.H2;
        DatabaseDriver UNKNOWN = DatabaseDriver.UNKNOWN;
        try(MockedStatic<DatabaseDriver> databaseDriverMockedStatic = mockStatic(DatabaseDriver.class)) {
            DataSource dataSource = mock(DataSource.class);
            Connection connection = mock(Connection.class);
            DatabaseMetaData metaData = mock(DatabaseMetaData.class);
            doReturn(connection).when(dataSource).getConnection();
            doReturn(metaData).when(connection).getMetaData();
            doReturn("test_url").when(metaData).getURL();
            databaseDriverMockedStatic.when(DatabaseDriver::values).thenCallRealMethod();

            databaseDriverMockedStatic.when(()->DatabaseDriver.fromJdbcUrl(any())).thenReturn(MYSQL);
            DBSpecificQueryProvider queryProvider = batchClusterAutoConfiguration.dbSpecificQueryProvider(dataSource);
            assertInstanceOf(MySQLDatabaseQueryProvider.class, queryProvider);

            databaseDriverMockedStatic.when(()->DatabaseDriver.fromJdbcUrl(any())).thenReturn(ORACLE);
            queryProvider = batchClusterAutoConfiguration.dbSpecificQueryProvider(dataSource);
            assertInstanceOf(OracleDatabaseQueryProvider.class, queryProvider);

            databaseDriverMockedStatic.when(()->DatabaseDriver.fromJdbcUrl(any())).thenReturn(POSTGRESQL);
            queryProvider = batchClusterAutoConfiguration.dbSpecificQueryProvider(dataSource);
            assertInstanceOf(PostgreSQLDatabaseQueryProvider.class, queryProvider);

            databaseDriverMockedStatic.when(()->DatabaseDriver.fromJdbcUrl(any())).thenReturn(UNKNOWN);
            Exception exception = assertThrows(BatchConfigurationException.class, ()->batchClusterAutoConfiguration.dbSpecificQueryProvider(dataSource));
            assertInstanceOf(BatchConfigurationException.class, exception);
            assertEquals("Unsupported Database Type UNKNOWN", exception.getMessage());
        }
    }

    @Test
    public void testClusterMonitoringScheduler() {
        TaskScheduler taskScheduler = batchClusterAutoConfiguration.clusterMonitoringScheduler();
        assertNotNull(taskScheduler);
        assertInstanceOf(ThreadPoolTaskScheduler.class, taskScheduler);
    }

    @Test
    public void testPartitionPollingScheduler() {
        TaskScheduler taskScheduler = batchClusterAutoConfiguration.partitionPollingScheduler();
        assertNotNull(taskScheduler);
        assertInstanceOf(ThreadPoolTaskScheduler.class, taskScheduler);
    }
}
