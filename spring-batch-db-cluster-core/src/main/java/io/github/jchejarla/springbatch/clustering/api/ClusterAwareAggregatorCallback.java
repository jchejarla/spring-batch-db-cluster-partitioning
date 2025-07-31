package io.github.jchejarla.springbatch.clustering.api;

import org.springframework.batch.core.StepExecution;

import java.util.Collection;

/**
 * A callback interface to be implemented by classes that need to react to the
 * successful completion or failure of a collection of Spring Batch partitioned
 * step executions in a clustered environment.
 * <p>
 * This interface allows for custom logic to be executed once all partitions
 * have finished, providing a centralized point to handle outcomes.
 * </p>
 */
public interface ClusterAwareAggregatorCallback {

    /**
     * Called when all partitioned step executions have completed successfully.
     *
     * @param executions A collection of {@link StepExecution} instances from the
     * partitioned steps that all completed successfully.
     */
    void onSuccess(Collection<StepExecution> executions);

    /**
     * Called when at least one of the partitioned step executions has failed.
     *
     * @param executions A collection of {@link StepExecution} instances from the
     * partitioned steps, including any that may have failed.
     */
    void onFailure(Collection<StepExecution> executions);
}
