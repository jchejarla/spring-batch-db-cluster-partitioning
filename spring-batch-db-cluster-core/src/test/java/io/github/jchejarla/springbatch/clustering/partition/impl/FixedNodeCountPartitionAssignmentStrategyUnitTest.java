package io.github.jchejarla.springbatch.clustering.partition.impl;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.partition.PartitionAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

public class FixedNodeCountPartitionAssignmentStrategyUnitTest extends BaseUnitTest {

    FixedNodeCountPartitionAssignmentStrategy fixedNodeCountPartitionAssignmentStrategy;

    @BeforeEach
    public void init() {
        fixedNodeCountPartitionAssignmentStrategy = Mockito.spy(new FixedNodeCountPartitionAssignmentStrategy(1));
    }

    @Test
    public void testAssignPartitions(){
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
        List<PartitionAssignment> partitionAssignments = fixedNodeCountPartitionAssignmentStrategy.assignPartitions(executionContexts, availableNodes);
        assertFalse(partitionAssignments.isEmpty());
        assertEquals(partitionAssignments.get(0).nodeId(),partitionAssignments.get(1).nodeId());
        assertEquals(partitionAssignments.get(2).nodeId(),partitionAssignments.get(3).nodeId());
    }

}
