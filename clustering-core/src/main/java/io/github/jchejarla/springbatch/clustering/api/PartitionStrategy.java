package io.github.jchejarla.springbatch.clustering.api;

import io.github.jchejarla.springbatch.clustering.partition.PartitioningMode;
import lombok.Builder;
import lombok.Getter;

/**
 * A builder class for configuring the partitioning strategy in a Spring Batch
 * clustered environment.  It encapsulates the partitioning mode and any
 * associated parameters.
 *
 * @author Janardhan Chejarla
 */
@Builder
@Getter
public class PartitionStrategy {
    /**
     * The partitioning mode to use.  This determines how partitions are assigned
     * to nodes in the cluster (e.g., round-robin, fixed node count).
     */
    PartitioningMode partitioningMode;
    /**
     * The fixed number of nodes to use for partitioning.  This is only
     * applicable when the {@link #partitioningMode} is set to
     * {@link PartitioningMode#FIXED_NODE_COUNT}.
     */
    int fixedNodeCount; // Specify only when the PartitioningMode is FIXED_NODE_COUNT
}
