package io.github.jchejarla.springbatch.clustering.partition;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.api.PartitionStrategy;
import io.github.jchejarla.springbatch.clustering.partition.impl.FixedNodeCountPartitionAssignmentStrategy;
import io.github.jchejarla.springbatch.clustering.partition.impl.RoundRobinPartitionAssignmentStrategy;
import io.github.jchejarla.springbatch.clustering.partition.impl.ScaleUpPartitionAssignmentStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class PartitionStrategyFactoryUnitTest extends BaseUnitTest {

    @Test
    public void testPartitionStrategyFactory() {
        PartitionStrategy partitionStrategy = PartitionStrategy.builder()
                .partitioningMode(PartitioningMode.FIXED_NODE_COUNT).build();
        PartitionAssignmentStrategy partitionAssignmentStrategy = PartitionStrategyFactory.getStrategy(partitionStrategy);
        assertInstanceOf(FixedNodeCountPartitionAssignmentStrategy.class, partitionAssignmentStrategy);
        partitionStrategy = PartitionStrategy.builder()
                .partitioningMode(PartitioningMode.SCALE_UP).build();
        partitionAssignmentStrategy = PartitionStrategyFactory.getStrategy(partitionStrategy);
        assertInstanceOf(ScaleUpPartitionAssignmentStrategy.class, partitionAssignmentStrategy);
        partitionStrategy = PartitionStrategy.builder()
                .partitioningMode(PartitioningMode.ROUND_ROBIN).build();
        partitionAssignmentStrategy = PartitionStrategyFactory.getStrategy(partitionStrategy);
        assertInstanceOf(RoundRobinPartitionAssignmentStrategy.class, partitionAssignmentStrategy);
    }
}
