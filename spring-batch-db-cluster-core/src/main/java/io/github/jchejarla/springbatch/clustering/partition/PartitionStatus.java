package io.github.jchejarla.springbatch.clustering.partition;

/**
 * Lifecycle states of a single partition row in {@code BATCH_PARTITIONS}.
 *
 * <p>The {@code name()} of each value is the exact string persisted in the {@code STATUS} column, and
 * must stay in sync with the {@code CHECK} constraint in the bundled schema DDL. This enum is the
 * single source of truth for partition status values across the framework.</p>
 *
 * @author Janardhan Chejarla
 */
public enum PartitionStatus {
    PENDING,
    CLAIMED,
    COMPLETED,
    FAILED
}
