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
package io.github.jchejarla.springbatch.clustering.api;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNode;
import io.github.jchejarla.springbatch.clustering.partition.ClusterPartitioningConstants;
import io.github.jchejarla.springbatch.clustering.partition.PartitioningMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.item.ExecutionContext;

import java.util.*;

public class ClusterAwarePartitionerUnitTest extends BaseUnitTest {

    @Mock
    DatabaseBackedClusterService databaseBackedClusterService;

    @Spy
    ClusterAwarePartitioner partitioner;

    @BeforeEach
    public void init() {
        partitioner.databaseBackedClusterService = databaseBackedClusterService;
    }

    @Test
    public void testPartitionSuccess() {
        List<ClusterNode> clusterNodeList = new ArrayList<>();
        clusterNodeList.add(new ClusterNode(UUID.randomUUID().toString(), 0));
        clusterNodeList.add(new ClusterNode(UUID.randomUUID().toString(), 0));
        Mockito.doReturn(clusterNodeList).when(databaseBackedClusterService).getActiveNodes();
        Mockito.doAnswer(invocationOnMock -> {
            List<ExecutionContext> executionContexts = new ArrayList<>();
            executionContexts.add(Mockito.mock(ExecutionContext.class));
            executionContexts.add(Mockito.mock(ExecutionContext.class));
            return executionContexts;
        }).when(partitioner).createDistributedPartitions(Mockito.anyInt());
        Map<String, ExecutionContext> partitionContexts = partitioner.partition(2);
        Assertions.assertNotNull(partitionContexts);
        Assertions.assertEquals(2, partitionContexts.size());
    }

    @Test
    public void testPartitionWhenNodesNotFound(){
        Mockito.doReturn(Collections.emptyList()).when(databaseBackedClusterService).getActiveNodes();
        Exception exception = Assertions.assertThrows(BatchConfigurationException.class, ()->partitioner.partition(2));
        Assertions.assertEquals("Spring batch clustering is enabled, but nodes information is not available in the DB, something is not right with nodes registration/configuration, please check...",
                exception.getMessage());
    }

    @Test
    public void testPartitionWhenNodesWithFixedNodes(){
        List<ClusterNode> clusterNodeList = new ArrayList<>();
        clusterNodeList.add(new ClusterNode(UUID.randomUUID().toString(), 0));
        clusterNodeList.add(new ClusterNode(UUID.randomUUID().toString(), 0));
        Mockito.doReturn(clusterNodeList).when(databaseBackedClusterService).getActiveNodes();
        Mockito.doAnswer(invocationOnMock -> {
            List<ExecutionContext> executionContexts = new ArrayList<>();
            executionContexts.add(new ExecutionContext());
            executionContexts.add(new ExecutionContext());
            return executionContexts;
        }).when(partitioner).createDistributedPartitions(Mockito.anyInt());
        Mockito.doReturn(PartitionStrategy.builder().partitioningMode(PartitioningMode.FIXED_NODE_COUNT).fixedNodeCount(2).build())
                .when(partitioner).buildPartitionStrategy();
        Map<String, ExecutionContext> partitionContexts = partitioner.partition(2);
        Assertions.assertNotNull(partitionContexts);
        Assertions.assertEquals(2, partitionContexts.size());
        ExecutionContext firstExecContext = partitionContexts.get("0");
        ExecutionContext secondExecContext = partitionContexts.get("1");
        Assertions.assertNotEquals(firstExecContext.get(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER), secondExecContext.get(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER));
    }

    @Test
    public void testPartitionWhenNodesWithFixedNodesButNodeCountIsNotSpecified(){
        List<ClusterNode> clusterNodeList = new ArrayList<>();
        clusterNodeList.add(new ClusterNode(UUID.randomUUID().toString(), 0));
        clusterNodeList.add(new ClusterNode(UUID.randomUUID().toString(), 0));
        Mockito.doReturn(clusterNodeList).when(databaseBackedClusterService).getActiveNodes();
        Mockito.doAnswer(invocationOnMock -> {
            List<ExecutionContext> executionContexts = new ArrayList<>();
            executionContexts.add(new ExecutionContext());
            executionContexts.add(new ExecutionContext());
            return executionContexts;
        }).when(partitioner).createDistributedPartitions(Mockito.anyInt());
        PartitionStrategy partitionStrategy = PartitionStrategy.builder().partitioningMode(PartitioningMode.FIXED_NODE_COUNT).build();
        Mockito.doReturn(partitionStrategy)
                .when(partitioner).buildPartitionStrategy();
        Map<String, ExecutionContext> partitionContexts = partitioner.partition(2);
        Assertions.assertNotNull(partitionContexts);
        Assertions.assertEquals(2, partitionContexts.size());
        ExecutionContext firstExecContext = partitionContexts.get("0");
        ExecutionContext secondExecContext = partitionContexts.get("1");
        Assertions.assertEquals(firstExecContext.get(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER), secondExecContext.get(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER));
    }
}
