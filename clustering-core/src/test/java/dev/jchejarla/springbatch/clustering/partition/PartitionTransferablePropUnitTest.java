package dev.jchejarla.springbatch.clustering.partition;

import dev.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PartitionTransferablePropUnitTest extends BaseUnitTest {

    @Test
    public void testPartitionTransferableProp() {
        PartitionTransferableProp partitionTransferableProp = PartitionTransferableProp.YES;
        assertEquals(ClusterPartitioningConstants.IS_TRANSFERABLE, partitionTransferableProp.key);
        assertEquals(Boolean.TRUE, partitionTransferableProp.val);
        partitionTransferableProp = PartitionTransferableProp.NO;
        assertEquals(ClusterPartitioningConstants.IS_TRANSFERABLE, partitionTransferableProp.key);
        assertEquals(Boolean.FALSE, partitionTransferableProp.val);
    }
}
