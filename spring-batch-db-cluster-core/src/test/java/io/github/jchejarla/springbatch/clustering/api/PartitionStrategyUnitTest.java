package io.github.jchejarla.springbatch.clustering.api;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.partition.PartitioningMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PartitionStrategyUnitTest extends BaseUnitTest {

    @Test
    public void testFixedNodeStrategyBuilder() {
        PartitionStrategy partitionStrategy = PartitionStrategy.builder()
                .partitioningMode(PartitioningMode.FIXED_NODE_COUNT)
                .fixedNodeCount(1)
                .build();
        Assertions.assertEquals(PartitioningMode.FIXED_NODE_COUNT, partitionStrategy.getPartitioningMode());
        Assertions.assertEquals(1, partitionStrategy.getFixedNodeCount());
    }

    @Test
    public void testRoundRobinStrategyBuilder() {
        PartitionStrategy partitionStrategy = PartitionStrategy.builder()
                .partitioningMode(PartitioningMode.ROUND_ROBIN)
                .build();
        Assertions.assertEquals(PartitioningMode.ROUND_ROBIN, partitionStrategy.getPartitioningMode());
    }

    @Test
    public void testScaleUpStrategyBuilder() {
        PartitionStrategy partitionStrategy = PartitionStrategy.builder()
                .partitioningMode(PartitioningMode.SCALE_UP)
                .build();
        Assertions.assertEquals(PartitioningMode.SCALE_UP, partitionStrategy.getPartitioningMode());
    }

}
