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

import io.github.jchejarla.springbatch.clustering.partition.PartitionAssignment;
import io.github.jchejarla.springbatch.clustering.partition.PartitionAssignmentStrategy;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A partition strategy that distributes partitions across available nodes in a scaling up manner.
 * <p>
 * This strategy assigns partitions to nodes in a simple round-robin fashion,
 * effectively scaling up the number of nodes used as the number of partitions increases.
 * </p>
 *
 * @author Janardhan Chejarla
 */
public class ScaleUpPartitionAssignmentStrategy implements PartitionAssignmentStrategy {

    /**
     * Assigns partitions to nodes using a scaling up approach.
     *
     * @param executionContexts The list of {@link ExecutionContext} instances representing the partitions.
     * @param availableNodes    The list of available node IDs in the cluster.
     * @return A list of {@link PartitionAssignment} objects representing the assignment of partitions to nodes.
     */
    @Override
    public List<PartitionAssignment> assignPartitions(List<ExecutionContext> executionContexts, List<String> availableNodes) {
        List<PartitionAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < executionContexts.size(); i++) {
            String assignedNode = availableNodes.get(i % availableNodes.size());
            assignments.add(new PartitionAssignment(i, executionContexts.get(i), assignedNode));
        }
        return assignments;
    }

}
