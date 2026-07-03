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
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class LeastLoadedPartitionAssignmentStrategyUnitTest extends BaseUnitTest {

    private final LeastLoadedPartitionAssignmentStrategy strategy = new LeastLoadedPartitionAssignmentStrategy();

    @Test
    public void testBusyNodesAreAvoided() {
        List<ExecutionContext> partitions = partitions(4);
        List<ClusterNode> nodes = List.of(
                new ClusterNode("busy", 10),
                new ClusterNode("idle-a", 0),
                new ClusterNode("idle-b", 0));

        List<PartitionAssignment> assignments = strategy.assignPartitions(partitions, nodes);

        assertEquals(4, assignments.size());
        long busy = assignments.stream().filter(a -> a.nodeId().equals("busy")).count();
        assertEquals(0, busy, "the already-busy node should get no new partitions here");
    }

    @Test
    public void testEqualLoadSpreadsEvenly() {
        List<ExecutionContext> partitions = partitions(6);
        List<ClusterNode> nodes = List.of(
                new ClusterNode("a", 0), new ClusterNode("b", 0), new ClusterNode("c", 0));

        List<PartitionAssignment> assignments = strategy.assignPartitions(partitions, nodes);

        assertEquals(6, assignments.size());
        for (String id : List.of("a", "b", "c")) {
            assertEquals(2, assignments.stream().filter(a -> a.nodeId().equals(id)).count());
        }
    }

    private List<ExecutionContext> partitions(int count) {
        List<ExecutionContext> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(mock(ExecutionContext.class));
        }
        return list;
    }
}
