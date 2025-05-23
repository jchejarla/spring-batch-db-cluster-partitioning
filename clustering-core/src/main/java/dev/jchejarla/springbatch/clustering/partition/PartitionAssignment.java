package dev.jchejarla.springbatch.clustering.partition;

import org.springframework.batch.item.ExecutionContext;

public record PartitionAssignment(int uniqueChunkId, ExecutionContext executionContext, String nodeId) {}
