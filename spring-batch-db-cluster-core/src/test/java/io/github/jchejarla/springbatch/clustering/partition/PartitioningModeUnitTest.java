package io.github.jchejarla.springbatch.clustering.partition;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PartitioningModeUnitTest extends BaseUnitTest {

    @Test
    public void testPartitioningMode() {
        PartitioningMode partitioningMode= PartitioningMode.FIXED_NODE_COUNT;
        assertEquals(PartitioningMode.FIXED_NODE_COUNT, partitioningMode);
        partitioningMode= PartitioningMode.SCALE_UP;
        assertEquals(PartitioningMode.SCALE_UP, partitioningMode);
        partitioningMode= PartitioningMode.ROUND_ROBIN;
        assertEquals(PartitioningMode.ROUND_ROBIN, partitioningMode);
    }
}
