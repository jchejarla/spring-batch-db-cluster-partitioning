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

import io.github.jchejarla.springbatch.clustering.partition.PartitioningMode;
import lombok.Builder;
import lombok.Getter;

/**
 * A builder class for configuring the partitioning strategy in a Spring Batch
 * clustered environment.  It encapsulates the partitioning mode and any
 * associated parameters.
 *
 * @author Janardhan Chejarla
 */
@Builder
@Getter
public class PartitionStrategy {
    /**
     * The partitioning mode to use.  This determines how partitions are assigned
     * to nodes in the cluster (e.g., round-robin, fixed node count).
     */
    PartitioningMode partitioningMode;
    /**
     * The fixed number of nodes to use for partitioning.  This is only
     * applicable when the {@link #partitioningMode} is set to
     * {@link PartitioningMode#FIXED_NODE_COUNT}.
     */
    int fixedNodeCount; // Specify only when the PartitioningMode is FIXED_NODE_COUNT
}
