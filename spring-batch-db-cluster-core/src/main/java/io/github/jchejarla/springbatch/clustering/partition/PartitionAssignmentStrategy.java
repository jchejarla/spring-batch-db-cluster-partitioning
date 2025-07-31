package io.github.jchejarla.springbatch.clustering.partition;

import org.springframework.batch.item.ExecutionContext;

import java.util.List;

public interface PartitionAssignmentStrategy {
    List<PartitionAssignment> assignPartitions(List<ExecutionContext> executionContexts, List<String> availableNodes);
}
