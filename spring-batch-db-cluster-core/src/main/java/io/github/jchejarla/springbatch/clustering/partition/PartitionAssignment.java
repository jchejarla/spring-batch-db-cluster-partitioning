package io.github.jchejarla.springbatch.clustering.partition;


import org.springframework.batch.infrastructure.item.ExecutionContext;

public record PartitionAssignment(int uniqueChunkId, ExecutionContext executionContext, String nodeId) {}
