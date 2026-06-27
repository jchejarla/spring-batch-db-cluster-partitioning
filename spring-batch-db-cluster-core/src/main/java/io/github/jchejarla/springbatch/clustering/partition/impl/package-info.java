/**
 * Built-in partition-assignment strategies: round-robin, fixed-node-count, and scale-up.
 *
 * <p>Each maps the work units produced by the partitioner onto the available nodes; the strategy is
 * selected at runtime via the partitioner's chosen {@code PartitionStrategy}.</p>
 */
package io.github.jchejarla.springbatch.clustering.partition.impl;
