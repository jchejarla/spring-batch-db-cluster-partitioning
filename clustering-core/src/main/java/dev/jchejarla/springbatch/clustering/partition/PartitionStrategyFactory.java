package dev.jchejarla.springbatch.clustering.partition;

import dev.jchejarla.springbatch.clustering.api.PartitionStrategy;
import dev.jchejarla.springbatch.clustering.partition.impl.*;

public class PartitionStrategyFactory {

    public static PartitionAssignmentStrategy getStrategy(PartitionStrategy partitionStrategy) {
       PartitioningMode mode = partitionStrategy.getPartitioningMode();
        switch (mode) {
            case SCALE_UP -> {
                return new ScaleUpPartitionAssignmentStrategy();
            }
            case FIXED_NODE_COUNT -> {
                return new FixedNodeCountPartitionAssignmentStrategy(partitionStrategy.getFixedNodeCount());
            }
            case ROUND_ROBIN -> {
                return new RoundRobinPartitionAssignmentStrategy();
            }
            default -> throw new IllegalArgumentException("Unknown partition strategy: " + mode);
        }
    }

}
