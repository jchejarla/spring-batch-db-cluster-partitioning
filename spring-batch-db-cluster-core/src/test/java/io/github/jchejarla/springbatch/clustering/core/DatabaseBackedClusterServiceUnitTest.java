package io.github.jchejarla.springbatch.clustering.core;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.MySQLDatabaseQueryProvider;
import io.github.jchejarla.springbatch.clustering.polling.PartitionAssignmentTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DatabaseBackedClusterServiceUnitTest extends BaseUnitTest {

    @Mock
    JdbcTemplate jdbcTemplate;
    @Mock
    BatchClusterProperties batchClusterProperties;

    DatabaseBackedClusterService databaseBackedClusterService;

    @BeforeEach
    public void init() {
        doReturn("Test-Node-Id").when(batchClusterProperties).getNodeId();
        databaseBackedClusterService = spy(new DatabaseBackedClusterService(jdbcTemplate, batchClusterProperties, new MySQLDatabaseQueryProvider()));
    }

    @Test
    public void testRegister() {
        databaseBackedClusterService.registerNode();
        verify(jdbcTemplate, times(1)).update(anyString(), anyString(), any(Date.class), any(Date.class), anyString(), anyString());
    }

    @Test
    public void testUpdateNodeHeartbeat() {
        databaseBackedClusterService.updateNodeHeartbeat();
        verify(jdbcTemplate, times(1)).update(anyString(), any(Date.class), anyString(), anyLong(), anyString());
    }

    @Test
    public void testMarkNodesUnreachable() {
        databaseBackedClusterService.markNodesUnreachable();
        verify(jdbcTemplate, times(1)).update(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    public void testDeleteNodesUnreachable() {
        databaseBackedClusterService.deleteNodesUnreachable();
        verify(jdbcTemplate, times(1)).update(anyString(), anyString(), anyLong());
    }

    @Test
    public void testSaveBatchJobCoordinationInfo() {
        databaseBackedClusterService.saveBatchJobCoordinationInfo(123L, 124L, "masterStep");
        verify(jdbcTemplate, times(1)).update(anyString(), anyLong(), anyString(), anyLong(), anyString(), anyString(), any(Date.class), any(Date.class));
    }

    @Test
    public void testUpdateBatchJobCoordinationStatus() {
        databaseBackedClusterService.updateBatchJobCoordinationStatus(123L, 124L, "STARTED");
        verify(jdbcTemplate, times(1)).update(anyString(), anyString(), any(Date.class), anyLong(), anyLong());
    }

    @Test
    public void testSaveBatchPartitions() {
        databaseBackedClusterService.saveBatchPartitions(Collections.emptyList());
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
    }

    @Test
    public void testUpdateBatchPartitionsToReAssignedNodes() {
        databaseBackedClusterService.updateBatchPartitionsToReAssignedNodes(Collections.emptyList());
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
    }

    @Test
    public void testGetPendingTasksCount() {
        databaseBackedClusterService.getPendingTasksCount(124L);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), any(Class.class), anyLong());
    }

    @Test
    public void testFetchPartitionAssignedTasks() {
        databaseBackedClusterService.fetchPartitionAssignedTasks();
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), anyString());
    }

    @Test
    public void testUpdatePartitionsStatus() {
        databaseBackedClusterService.updatePartitionStatus(Mockito.mock(PartitionAssignmentTask.class), "COMPLETED");
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
    }

    @Test
    public void testUpdatePartitionStatus() {
        databaseBackedClusterService.updatePartitionsStatus(Collections.emptyList(), "CLAIMED");
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
    }

    @Test
    public void testCheckForOrphanedTasks() {
        databaseBackedClusterService.checkForOrphanedTasks(124L);
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), anyLong(), anyLong());
    }

    @Test
    public void testGetActiveNodes() {
        databaseBackedClusterService.getActiveNodes();
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
    }
}
