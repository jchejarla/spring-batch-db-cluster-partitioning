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
package io.github.jchejarla.springbatch.clustering.partition.impl;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNode;
import io.github.jchejarla.springbatch.clustering.partition.PartitionAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

public class FixedNodeCountPartitionAssignmentStrategyUnitTest extends BaseUnitTest {

    FixedNodeCountPartitionAssignmentStrategy fixedNodeCountPartitionAssignmentStrategy;

    @BeforeEach
    public void init() {
        fixedNodeCountPartitionAssignmentStrategy = Mockito.spy(new FixedNodeCountPartitionAssignmentStrategy(1));
    }

    @Test
    public void testAssignPartitions(){
        List<ExecutionContext> executionContexts = new ArrayList<>();
        executionContexts.add(mock(ExecutionContext.class));
        executionContexts.add(mock(ExecutionContext.class));
        executionContexts.add(mock(ExecutionContext.class));
        executionContexts.add(mock(ExecutionContext.class));
        List<ClusterNode> availableNodes = new ArrayList<>();
        availableNodes.add(new ClusterNode("Test-Node-123", 0));
        availableNodes.add(new ClusterNode("Test-Node-124", 0));
        availableNodes.add(new ClusterNode("Test-Node-125", 0));
        availableNodes.add(new ClusterNode("Test-Node-126", 0));
        availableNodes.add(new ClusterNode("Test-Node-127", 0));
        List<PartitionAssignment> partitionAssignments = fixedNodeCountPartitionAssignmentStrategy.assignPartitions(executionContexts, availableNodes);
        assertFalse(partitionAssignments.isEmpty());
        assertEquals(partitionAssignments.get(0).nodeId(),partitionAssignments.get(1).nodeId());
        assertEquals(partitionAssignments.get(2).nodeId(),partitionAssignments.get(3).nodeId());
    }

}
