package dev.jchejarla.springbatch.clustering.partition;

import dev.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class PartitionAssignmentUnitTest extends BaseUnitTest {

    @Test
    public void testPartitionAssignmentRecord() {
        PartitionAssignment partitionAssignment = new PartitionAssignment(123, mock(ExecutionContext.class), "Test-Node-123");
        assertNotNull(partitionAssignment.executionContext());
        assertNotNull(partitionAssignment.nodeId());
        assertEquals(123, partitionAssignment.uniqueChunkId());
    }
}
