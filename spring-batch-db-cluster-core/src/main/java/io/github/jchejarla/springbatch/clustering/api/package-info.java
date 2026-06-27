/**
 * User-facing extension points for building cluster-aware partitioned jobs.
 *
 * <p>Subclass {@code ClusterAwarePartitioner} to split work and choose a {@code PartitionStrategy},
 * and implement {@code ClusterAwareAggregator} / {@code ClusterAwareAggregatorCallback} to aggregate
 * partition results and react to overall job success or failure. These are the only types most
 * applications need to interact with; the rest of the library is wired automatically.</p>
 */
package io.github.jchejarla.springbatch.clustering.api;
