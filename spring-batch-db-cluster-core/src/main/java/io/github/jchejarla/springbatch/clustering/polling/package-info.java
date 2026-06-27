/**
 * Worker-side task polling and execution.
 *
 * <p>Each node polls for partitions assigned to it, claims them transactionally, and executes the
 * corresponding Spring Batch step concurrently (up to the per-node limit), recording each
 * partition's completion or failure back to the coordination tables.</p>
 */
package io.github.jchejarla.springbatch.clustering.polling;
