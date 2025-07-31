package io.github.jchejarla.springbatch.clustering.polling;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PartitionAssignmentTaskUnitTest extends BaseUnitTest {

    @Test
    public void testPartitionAssignmentTask() {
        PartitionAssignmentTask partitionAssignmentTask = new PartitionAssignmentTask(123L, "Test-Step", 124L, 125L,
                true, "Test-Master", "Test-Node-123");
        assertEquals(123L, partitionAssignmentTask.jobExecutionId());
        assertEquals("Test-Step", partitionAssignmentTask.stepName());
        assertEquals(124L, partitionAssignmentTask.stepExecutionId());
        assertEquals(125L, partitionAssignmentTask.masterStepExecutionId());
        assertTrue(partitionAssignmentTask.isTransferable());
        assertEquals("Test-Master", partitionAssignmentTask.masterStepName());
        assertEquals("Test-Node-123", partitionAssignmentTask.assignedNode());
    }
}
