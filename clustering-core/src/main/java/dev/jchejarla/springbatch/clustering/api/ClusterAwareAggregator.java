package dev.jchejarla.springbatch.clustering.api;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.DefaultStepExecutionAggregator;
import org.springframework.batch.core.partition.support.RemoteStepExecutionAggregator;

import java.util.Collection;

public class ClusterAwareAggregator extends RemoteStepExecutionAggregator {

    private final ClusterAwareAggregatorCallback clusterAwareAggregatorCallback;

    public ClusterAwareAggregator(ClusterAwareAggregatorCallback clusterAwareAggregatorCallback) {
        this.clusterAwareAggregatorCallback = clusterAwareAggregatorCallback;
        setDelegate(new ClusterAwareClusterAwareAggregatorInternal());
    }

    public void doHandleSuccess(Collection<StepExecution> executions) {
        clusterAwareAggregatorCallback.onSuccess(executions);
    }

    public void doHandleFailed(Collection<StepExecution> executions) {
        clusterAwareAggregatorCallback.onFailure(executions);
    }


    private class ClusterAwareClusterAwareAggregatorInternal extends DefaultStepExecutionAggregator {

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
