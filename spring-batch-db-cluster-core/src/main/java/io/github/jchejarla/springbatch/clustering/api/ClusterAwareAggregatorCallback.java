/*
 * Copyright 2025 Janardhan Chejarla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.jchejarla.springbatch.clustering.api;

import org.springframework.batch.core.step.StepExecution;

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
