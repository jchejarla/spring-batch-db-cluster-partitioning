package dev.jchejarla.springbatch.clustering.api;

import org.springframework.batch.core.StepExecution;

import java.util.Collection;

public interface ClusterAwareAggregatorCallback {
    void onSuccess(Collection<StepExecution> executions);
    void onFailure(Collection<StepExecution> executions);
}
