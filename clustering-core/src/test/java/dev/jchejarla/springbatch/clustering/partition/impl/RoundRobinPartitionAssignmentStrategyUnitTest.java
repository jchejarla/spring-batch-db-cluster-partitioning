package dev.jchejarla.springbatch.clustering.partition.impl;

import dev.jchejarla.springbatch.clustering.BaseUnitTest;
import dev.jchejarla.springbatch.clustering.partition.PartitionAssignment;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class RoundRobinPartitionAssignmentStrategyUnitTest extends BaseUnitTest {

    @Spy
    RoundRobinPartitionAssignmentStrategy partitionAssignmentStrategy;

    @Test
    public void testRoundRobinPartitionAssignmentStrategy() {
        List<ExecutionContext> executionContexts = new ArrayList<>();
        executionContexts.add(mock(ExecutionContext.class));
        executionContexts.add(mock(ExecutionContext.class));
        executionContexts.add(mock(ExecutionContext.class));
        executionContexts.add(mock(ExecutionContext.class));
        List<String> availableNodes = new ArrayList<>();
        availableNodes.add("Test-Node-123");
        availableNodes.add("Test-Node-124");
        availableNodes.add("Test-Node-125");
        availableNodes.add("Test-Node-126");
        availableNodes.add("Test-Node-127");
        List<PartitionAssignment> partitionAssignments = partitionAssignmentStrategy.assignPartitions(executionContexts, availableNodes);
        assertFalse(partitionAssignments.isEmpty());
        assertNotEquals(partitionAssignments.get(0).nodeId(),partitionAssignments.get(1).nodeId());
        assertNotEquals(partitionAssignments.get(2).nodeId(),partitionAssignments.get(3).nodeId());
    }
}
