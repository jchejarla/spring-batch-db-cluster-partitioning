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
import io.github.jchejarla.springbatch.clustering.partition.PartitionAssignment;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

public class ScaleUpPartitionAssignmentStrategyUnitTest extends BaseUnitTest {

    @Spy
    ScaleUpPartitionAssignmentStrategy scaleUpPartitionAssignmentStrategy;

    @Test
    public void testScaleUpPartitionAssignmentStrategy() {
        List<ExecutionContext> executionContexts = new ArrayList<>();
        executionContexts.add(mock(ExecutionContext.class));
        executionContexts.add(mock(ExecutionContext.class));
        executionContexts.add(mock(ExecutionContext.class));
        executionContexts.add(mock(ExecutionContext.class));
        List<String> availableNodes = new ArrayList<>();
        availableNodes.add("Test-Node-123");
        availableNodes.add("Test-Node-124");
        availableNodes.add("Test-Node-125");
        availableNodes.add("Test-Node-126");
        availableNodes.add("Test-Node-127");
        List<PartitionAssignment> partitionAssignments = scaleUpPartitionAssignmentStrategy.assignPartitions(executionContexts, availableNodes);
        assertFalse(partitionAssignments.isEmpty());
        assertNotEquals(partitionAssignments.get(0).nodeId(),partitionAssignments.get(1).nodeId());
        assertNotEquals(partitionAssignments.get(2).nodeId(),partitionAssignments.get(3).nodeId());
    }

}
