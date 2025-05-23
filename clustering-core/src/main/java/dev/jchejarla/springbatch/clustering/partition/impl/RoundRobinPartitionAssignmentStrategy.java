package dev.jchejarla.springbatch.clustering.partition.impl;

import dev.jchejarla.springbatch.clustering.partition.PartitionAssignment;
import dev.jchejarla.springbatch.clustering.partition.PartitionAssignmentStrategy;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A partition strategy that assigns partitions to nodes in a round-robin fashion.
 * <p>
 * This strategy iterates through the available nodes, assigning each partition
 * to the next node in the list.  It ensures an even distribution of partitions
 * across the nodes.
 * </p>
 *
 * @author Janardhan Chejarla
 */
public class RoundRobinPartitionAssignmentStrategy implements PartitionAssignmentStrategy {

    /**
     * Assigns partitions to nodes using a round-robin approach.
     *
     * @param executionContexts The list of {@link ExecutionContext} instances representing the partitions.
     * @param availableNodes    The list of available node IDs in the cluster.
     * @return A list of {@link PartitionAssignment} objects representing the assignment of partitions to nodes.
     */
    @Override
    public List<PartitionAssignment> assignPartitions(List<ExecutionContext> executionContexts, List<String> availableNodes) {
        List<PartitionAssignment> assignments = new ArrayList<>();
        int nodeIndex = 0;

        for (int i = 0; i < executionContexts.size(); i++) {
            String node = availableNodes.get(nodeIndex++);
            if (nodeIndex == availableNodes.size()) {
                nodeIndex = 0; // Wrap around
            }
            assignments.add(new PartitionAssignment(i, executionContexts.get(i), node));
        }

        return assignments;
    }

}
