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
package io.github.jchejarla.springbatch.clustering.mgmt;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.scheduling.TaskScheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ClusterNodeManagerUnitTest extends BaseUnitTest {

    @Mock
    DatabaseBackedClusterService databaseBackedClusterService;
    @Mock
    BatchClusterProperties batchClusterProperties;
    @Mock
    TaskScheduler taskScheduler;
    @Mock
    ClusterNodeInfo clusterNodeInfo;
    @Mock
    ClusterNodeStatusChangeConditionNotifier clusterNodeStatusChangeConditionNotifier;

    ClusterNodeManager clusterNodeManager;

    @BeforeEach
    public void init() {
        clusterNodeManager = new ClusterNodeManager(databaseBackedClusterService, batchClusterProperties, taskScheduler, clusterNodeInfo, clusterNodeStatusChangeConditionNotifier);
    }

    @Test
    public void testStartSuccess() {
        doReturn(1).when(databaseBackedClusterService).registerNode();
        clusterNodeManager.start();
        verify(taskScheduler, times(4)).scheduleAtFixedRate(Mockito.any(), Mockito.any());
    }

    @Test
    public void testStartFails() {
        doReturn(0).when(databaseBackedClusterService).registerNode();
        Exception exception = Assertions.assertThrows(BatchConfigurationException.class, ()->clusterNodeManager.start());
        assertTrue(exception.getMessage().startsWith("Application failed to register the node with id"));
    }

    @Test
    public void testUpdateHeartbeatSuccess() {
        doReturn(true).when(batchClusterProperties).isTracingEnabled();
        doReturn(1).when(databaseBackedClusterService).updateNodeHeartbeat();
        assertDoesNotThrow(()->clusterNodeManager.updateHeartbeat());
        verify(databaseBackedClusterService, times(1)).updateNodeHeartbeat();
    }

    @Test
    public void testUpdateHeartbeatFailsReattemptToRegister() {
        doReturn(0).when(databaseBackedClusterService).updateNodeHeartbeat();
        assertDoesNotThrow(()->clusterNodeManager.updateHeartbeat());
        verify(databaseBackedClusterService, times(1)).updateNodeHeartbeat();
        verify(databaseBackedClusterService, times(1)).registerNode();
    }

    @Test
    public void testUpdateHeartbeatFailsReattemptToRegisterAlsoFails() {
        doReturn(0).when(databaseBackedClusterService).updateNodeHeartbeat();
        doReturn(1).when(databaseBackedClusterService).registerNode();
        assertDoesNotThrow(()->clusterNodeManager.updateHeartbeat());
        verify(databaseBackedClusterService, times(1)).updateNodeHeartbeat();
        verify(databaseBackedClusterService, times(1)).registerNode();
        verify(batchClusterProperties, times(0)).isTracingEnabled();
    }

    @Test
    public void testUpdateHeartbeatThrowsException() {
        doThrow(RuntimeException.class).when(databaseBackedClusterService).updateNodeHeartbeat();
        assertDoesNotThrow(()->clusterNodeManager.updateHeartbeat());
        verify(databaseBackedClusterService, times(1)).updateNodeHeartbeat();
        verify(databaseBackedClusterService, times(0)).registerNode();
        verify(batchClusterProperties, times(0)).isTracingEnabled();
    }

    @Test
    public void testMarkNodesUnreachableSuccess() {
        doReturn(1).when(databaseBackedClusterService).markNodesUnreachable();
        clusterNodeManager.markNodesUnreachable();
        verify(databaseBackedClusterService, times(1)).markNodesUnreachable();
    }

    @Test
    public void testMarkNodesUnreachableFails() {
        doReturn(true).when(batchClusterProperties).isTracingEnabled();
        doReturn(0).when(databaseBackedClusterService).markNodesUnreachable();
        clusterNodeManager.markNodesUnreachable();
        verify(databaseBackedClusterService, times(1)).markNodesUnreachable();
    }

    @Test
    public void testMarkNodesUnreachableThrowsException() {
        doThrow(RuntimeException.class).when(databaseBackedClusterService).markNodesUnreachable();
        clusterNodeManager.markNodesUnreachable();
        verify(databaseBackedClusterService, times(1)).markNodesUnreachable();
    }

    @Test
    public void testRemoveNodesUnreachable() {
        doReturn(1).when(databaseBackedClusterService).deleteNodesUnreachable();
        clusterNodeManager.removeNodesUnreachable();
        verify(databaseBackedClusterService, times(1)).deleteNodesUnreachable();
    }

    @Test
    public void testRemoveNodesUnreachableFails() {
        doReturn(true).when(batchClusterProperties).isTracingEnabled();
        doReturn(1).when(databaseBackedClusterService).deleteNodesUnreachable();
        clusterNodeManager.removeNodesUnreachable();
        verify(databaseBackedClusterService, times(1)).deleteNodesUnreachable();
    }

    @Test
    public void testRemoveNodesUnreachableThrowsException() {
        doThrow(RuntimeException.class).when(databaseBackedClusterService).deleteNodesUnreachable();
        clusterNodeManager.removeNodesUnreachable();
        verify(databaseBackedClusterService, times(1)).deleteNodesUnreachable();
    }
}
