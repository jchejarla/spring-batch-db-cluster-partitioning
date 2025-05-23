package dev.jchejarla.springbatch.clustering.api;

import dev.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import dev.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import dev.jchejarla.springbatch.clustering.mgmt.ClusterNode;
import dev.jchejarla.springbatch.clustering.partition.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNullApi;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An abstract Spring Batch {@link Partitioner} that enables distributed partitioning
 * in a clustered environment.  It dynamically assigns partitions to available nodes
 * in the cluster, using a shared database for coordination.  Subclasses must
 * implement methods to define how data is split into chunks and how partitioning
 * strategies are built.
 *
 * <p>This class provides the framework for:</p>
 * <ul>
 * <li>Fetching active cluster nodes and their load.</li>
 * <li>Dividing the workload into partitions.</li>
 * <li>Assigning partitions to nodes based on a chosen strategy.</li>
 * <li>Ensuring partition transferability in case of node failure.</li>
 * </ul>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 * <li>Cluster-aware:  Partitions are distributed across a cluster of Spring Batch instances.</li>
 * <li>Dynamic: Handles nodes joining and leaving the cluster.</li>
 * <li>Database-driven: Uses a shared database for cluster coordination and state management.</li>
 * <li>Extensible:  Abstract class requiring subclasses to define specific partitioning logic.</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <ol>
 * <li>Create a subclass of {@code ClusterAwarePartitioner}.</li>
 * <li>Implement the abstract methods:
 * <ul>
 * <li>{@link #splitIntoChunksForDistribution(int)}: Defines how data is split.</li>
 * <li>{@link #arePartitionsTransferableWhenNodeFailed()}:  Configures failover behavior.</li>
 * <li>{@link #buildPartitionStrategy()}:  Selects the partitioning strategy.</li>
 * </ul>
 * </li>
 * <li>Configure the Spring Batch job to use your custom partitioner.</li>
 * </ol>
 *
 * @author Janardhan Chejarla
 * @since 1.0
 */
@Slf4j
@ConditionalOnClusterEnabled
public abstract class ClusterAwarePartitioner implements Partitioner {

    @Autowired
    protected DatabaseBackedClusterService databaseBackedClusterService;

    /**
     * Partitions the input data into chunks and assigns them to available nodes
     * in the cluster.
     * <p>
     * This implementation overrides the standard Spring Batch {@link Partitioner#partition(int)}
     * method to delegate the partitioning logic to a {@link PartitionAssignmentStrategy}
     * obtained from the {@link PartitionStrategyFactory}. The number of partitions
     * is determined by the number of active nodes in the cluster, not by the
     * {@code gridSize} parameter.
     * </p>
     *
     * @param gridSize  Ignored in this implementation. The number of partitions
     * is determined by the number of active nodes.
     * @return  A map of partition names to {@link ExecutionContext} instances,
     * where each {@link ExecutionContext} contains information about
     * the assigned node and whether partitions are transferable on node failure.
     * @throws BatchConfigurationException If no active nodes are found in the database,
     * indicating a configuration issue.
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        List<ClusterNode> activeNodesOrderedByLoad = databaseBackedClusterService.getActiveNodes();

        if(activeNodesOrderedByLoad.isEmpty()) {
            log.error("Spring batch clustering is enabled, but nodes information is not available in the DB, something is not right with nodes registration/configuration, please check...");
            throw new BatchConfigurationException("Spring batch clustering is enabled, but nodes information is not available in the DB, something is not right with nodes registration/configuration, please check...");
        }
        List<ExecutionContext> executionContexts = splitIntoChunksForDistribution(activeNodesOrderedByLoad.size());

        List<String> activeNodes = activeNodesOrderedByLoad.stream().map(ClusterNode::nodeId).collect(Collectors.toList());
        PartitionStrategy partitionStrategy = buildPartitionStrategy();
        if(Objects.isNull(partitionStrategy)) {
            partitionStrategy = PartitionStrategy.builder().partitioningMode(PartitioningMode.ROUND_ROBIN).build();
        }

        if(partitionStrategy.getPartitioningMode() == PartitioningMode.FIXED_NODE_COUNT && partitionStrategy.getFixedNodeCount() == 0) {
            partitionStrategy = PartitionStrategy.builder().partitioningMode(PartitioningMode.FIXED_NODE_COUNT).fixedNodeCount(1).build();
        }

        log.info("Strategy for distributing the step workload {}", partitionStrategy.getPartitioningMode());
        PartitionAssignmentStrategy strategy = PartitionStrategyFactory.getStrategy(partitionStrategy);
        List<PartitionAssignment> assignments = strategy.assignPartitions(executionContexts, activeNodes);
        return assignments.stream().collect(Collectors.toMap(partitionAssignment-> String.valueOf(partitionAssignment.uniqueChunkId()), partitionAssignment-> {
            ExecutionContext executionContext = partitionAssignment.executionContext();
            executionContext.putString(ClusterPartitioningConstants.CLUSTER_NODE_IDENTIFIER, partitionAssignment.nodeId());
            executionContext.put(ClusterPartitioningConstants.IS_TRANSFERABLE_IDENTIFIER, arePartitionsTransferableWhenNodeFailed());
            return executionContext;
        }));
    }

    /**
     * Abstract method to be implemented by subclasses to define how the input data
     * should be split into chunks for distribution across the cluster nodes.
     *
     * @param availableNodeCount The number of nodes available in the cluster.
     * @return A list of {@link ExecutionContext} instances, each representing a chunk of data.
     */
    public abstract List<ExecutionContext> splitIntoChunksForDistribution(int availableNodeCount);

    /**
     * Abstract method to be implemented by subclasses to specify whether partitions
     * should be transferred to other nodes when a node fails.
     *
     * @return An enum value indicating whether partitions are transferable.
     */
    public abstract PartitionTransferableProp arePartitionsTransferableWhenNodeFailed();

    /**
     * Abstract method to be implemented by subclasses to define the partitioning
     * strategy to be used.
     *
     * @return A {@link PartitionStrategy} object configuring the partitioning strategy.
     */
    public abstract PartitionStrategy buildPartitionStrategy();

}
