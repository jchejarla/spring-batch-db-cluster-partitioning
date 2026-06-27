/**
 * Master-side partitioning.
 *
 * <p>Contains the cluster-aware {@code PartitionHandler} that persists partitions, monitors their
 * completion, and reassigns orphaned work, together with the partition-assignment strategy
 * abstraction that maps work units to the currently live nodes.</p>
 */
package io.github.jchejarla.springbatch.clustering.partition;
