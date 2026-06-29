package io.github.jchejarla.springbatch.clustering.core;

/**
 * Lifecycle states of a job-coordination row in {@code BATCH_JOB_COORDINATION}.
 *
 * <p>Normal flow is {@link #CREATED} &rarr; {@link #STARTED} &rarr; {@link #COMPLETED}. Recovery of a
 * job whose master node was lost moves it {@link #STARTED} &rarr; {@link #RECOVERING} (claimed by a
 * surviving node) &rarr; {@link #ABANDONED}. The {@code name()} of each value is the exact string
 * persisted in the {@code STATUS} column; this enum is the single source of truth for these values.</p>
 *
 * @author Janardhan Chejarla
 */
public enum CoordinationStatus {
    CREATED,
    STARTED,
    COMPLETED,
    RECOVERING,
    ABANDONED
}
