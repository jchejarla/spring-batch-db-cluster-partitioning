package io.github.jchejarla.springbatch.clustering.partition;

import io.github.jchejarla.springbatch.clustering.api.PartitionStrategy;
import io.github.jchejarla.springbatch.clustering.partition.impl.*;
import io.github.jchejarla.springbatch.clustering.partition.impl.FixedNodeCountPartitionAssignmentStrategy;
import io.github.jchejarla.springbatch.clustering.partition.impl.RoundRobinPartitionAssignmentStrategy;
import io.github.jchejarla.springbatch.clustering.partition.impl.ScaleUpPartitionAssignmentStrategy;

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
