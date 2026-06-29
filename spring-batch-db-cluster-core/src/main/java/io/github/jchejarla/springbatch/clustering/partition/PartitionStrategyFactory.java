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
package io.github.jchejarla.springbatch.clustering.partition;

import io.github.jchejarla.springbatch.clustering.api.PartitionStrategy;
import io.github.jchejarla.springbatch.clustering.partition.impl.*;
import io.github.jchejarla.springbatch.clustering.partition.impl.FixedNodeCountPartitionAssignmentStrategy;
import io.github.jchejarla.springbatch.clustering.partition.impl.RoundRobinPartitionAssignmentStrategy;
import io.github.jchejarla.springbatch.clustering.partition.impl.ScaleUpPartitionAssignmentStrategy;

public class PartitionStrategyFactory {

    public static PartitionAssignmentStrategy getStrategy(PartitionStrategy partitionStrategy) {
       PartitioningMode mode = partitionStrategy.getPartitioningMode();
        switch (mode) {
            case SCALE_UP -> {
                return new ScaleUpPartitionAssignmentStrategy();
            }
            case FIXED_NODE_COUNT -> {
                return new FixedNodeCountPartitionAssignmentStrategy(partitionStrategy.getFixedNodeCount());
            }
            case ROUND_ROBIN -> {
                return new RoundRobinPartitionAssignmentStrategy();
            }
            default -> throw new IllegalArgumentException("Unknown partition strategy: " + mode);
        }
    }

}
