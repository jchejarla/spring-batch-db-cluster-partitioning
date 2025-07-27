package io.github.jchejarla.springbatch.clustering.partition.impl;

import io.github.jchejarla.springbatch.clustering.partition.PartitionAssignment;
import io.github.jchejarla.springbatch.clustering.partition.PartitionAssignmentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A partition strategy that assigns partitions to a fixed number of nodes.
 * <p>
 * This strategy selects a specified number of nodes and distributes partitions
 * among them using a round-robin approach. If the number of available nodes
 * is less than the fixed node count, it uses all available nodes.
 * </p>
 *
 * @author Janardhan Chejarla
 */
@RequiredArgsConstructor
public class FixedNodeCountPartitionAssignmentStrategy implements PartitionAssignmentStrategy {

    private final int fixedNodeCount;

    /**
     * Assigns partitions to a fixed number of nodes using a round-robin approach.
     *
     * @param executionContexts The list of {@link ExecutionContext} instances representing the partitions.
     * @param availableNodes    The list of available node IDs in the cluster.
     * @return A list of {@link PartitionAssignment} objects representing the assignment of partitions to nodes.
     */
    @Override
    public List<PartitionAssignment> assignPartitions(List<ExecutionContext> executionContexts, List<String> availableNodes) {
        List<String> nodes = availableNodes.subList(0, Math.min(fixedNodeCount, availableNodes.size()));
        List<PartitionAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < executionContexts.size(); i++) {
            ExecutionContext executionContext = executionContexts.get(i);
            String assignedNode = nodes.get(i % nodes.size());
            assignments.add(new PartitionAssignment(i, executionContext, assignedNode));
        }
        return assignments;
    }

}
