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

import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNode;
import io.github.jchejarla.springbatch.clustering.partition.PartitionAssignment;
import io.github.jchejarla.springbatch.clustering.partition.PartitionAssignmentStrategy;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A load-aware strategy that assigns each partition to the node with the lowest current load.
 *
 * <p>Each node's running load is seeded from the live load it reported (the count of partitions it is
 * already executing, tracked in {@code BATCH_NODES}), then incremented locally as partitions are
 * assigned within this call. This spreads new work toward the least-busy nodes and accounts for work
 * a node is already doing for other jobs — unlike round-robin, which ignores existing load. Ties are
 * broken by node order for deterministic assignment.</p>
 *
 * @author Janardhan Chejarla
 */
public class LeastLoadedPartitionAssignmentStrategy implements PartitionAssignmentStrategy {

    @Override
    public List<PartitionAssignment> assignPartitions(List<ExecutionContext> executionContexts, List<ClusterNode> availableNodes) {
        int nodeCount = availableNodes.size();
        long[] runningLoad = new long[nodeCount];
        for (int j = 0; j < nodeCount; j++) {
            runningLoad[j] = availableNodes.get(j).currentLoad();
        }

        List<PartitionAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < executionContexts.size(); i++) {
            int target = 0;
            for (int j = 1; j < nodeCount; j++) {
                if (runningLoad[j] < runningLoad[target]) {
                    target = j;
                }
            }
            runningLoad[target]++;
            assignments.add(new PartitionAssignment(i, executionContexts.get(i), availableNodes.get(target).nodeId()));
        }
        return assignments;
    }

}
