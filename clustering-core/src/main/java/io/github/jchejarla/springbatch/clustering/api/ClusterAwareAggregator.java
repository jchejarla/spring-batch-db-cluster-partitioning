package io.github.jchejarla.springbatch.clustering.api;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.DefaultStepExecutionAggregator;
import org.springframework.batch.core.partition.support.RemoteStepExecutionAggregator;

import java.util.Collection;

/**
 * An extension of Spring Batch's {@link RemoteStepExecutionAggregator} that provides
 * custom callbacks for handling the success or failure of partitioned step executions
 * in a clustered environment.
 * <p>
 * This aggregator delegates the core aggregation logic to an internal
 * {@link DefaultStepExecutionAggregator} but intercepts the final status
 * to invoke specific success or failure callbacks defined by the
 * {@link ClusterAwareAggregatorCallback} interface.
 * </p>
 */
public class ClusterAwareAggregator extends RemoteStepExecutionAggregator {

    /**
     * The callback interface instance that will be notified upon the success or failure
     * of the aggregated step executions.
     */
    private final ClusterAwareAggregatorCallback clusterAwareAggregatorCallback;

    /**
     * Constructs a new {@code ClusterAwareAggregator} with the specified callback.
     *
     * @param clusterAwareAggregatorCallback The callback to be invoked on success or failure.
     */
    public ClusterAwareAggregator(ClusterAwareAggregatorCallback clusterAwareAggregatorCallback) {
        this.clusterAwareAggregatorCallback = clusterAwareAggregatorCallback;
        setDelegate(new ClusterAwareClusterAwareAggregatorInternal());
    }

    /**
     * Handles the success case for a collection of step executions.
     * This method simply delegates the call to the configured {@link ClusterAwareAggregatorCallback#onSuccess(Collection)}.
     *
     * @param executions A collection of {@link StepExecution} instances that have completed successfully.
     */
    public void doHandleSuccess(Collection<StepExecution> executions) {
        clusterAwareAggregatorCallback.onSuccess(executions);
    }

    /**
     * Handles the failed case for a collection of step executions.
     * This method simply delegates the call to the configured {@link ClusterAwareAggregatorCallback#onFailure(Collection)}.
     *
     * @param executions A collection of {@link StepExecution} instances where at least one has failed.
     */
    public void doHandleFailed(Collection<StepExecution> executions) {
        clusterAwareAggregatorCallback.onFailure(executions);
    }


    /**
     * An internal concrete implementation of {@link DefaultStepExecutionAggregator}
     * that overrides the {@code aggregate} method to trigger the custom success or failure
     * handlers based on the aggregated {@link StepExecution} status.
     */
    private class ClusterAwareClusterAwareAggregatorInternal extends DefaultStepExecutionAggregator {

        /**
         * Aggregates the results of multiple partitioned step executions into a single
         * master {@link StepExecution}. After the default aggregation, it checks the
         * status of the master step execution and invokes the appropriate
         * {@link ClusterAwareAggregator#doHandleSuccess(Collection)} or
         * {@link ClusterAwareAggregator#doHandleFailed(Collection)} method.
         *
         * @param result The master {@link StepExecution} to which the results will be aggregated.
         * @param executions A collection of {@link StepExecution} instances from the partitioned steps.
         */
        @Override
        public void aggregate(StepExecution result, Collection<StepExecution> executions) {
            super.aggregate(result, executions);
            if(result.getStatus() == BatchStatus.FAILED) {
                doHandleFailed(executions);
            } else {
                doHandleSuccess(executions);
            }
        }
    }
}
