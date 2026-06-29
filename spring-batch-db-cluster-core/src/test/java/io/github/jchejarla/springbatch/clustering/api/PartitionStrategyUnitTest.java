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

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import io.github.jchejarla.springbatch.clustering.partition.PartitioningMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PartitionStrategyUnitTest extends BaseUnitTest {

    @Test
    public void testFixedNodeStrategyBuilder() {
        PartitionStrategy partitionStrategy = PartitionStrategy.builder()
                .partitioningMode(PartitioningMode.FIXED_NODE_COUNT)
                .fixedNodeCount(1)
                .build();
        Assertions.assertEquals(PartitioningMode.FIXED_NODE_COUNT, partitionStrategy.getPartitioningMode());
        Assertions.assertEquals(1, partitionStrategy.getFixedNodeCount());
    }

    @Test
    public void testRoundRobinStrategyBuilder() {
        PartitionStrategy partitionStrategy = PartitionStrategy.builder()
                .partitioningMode(PartitioningMode.ROUND_ROBIN)
                .build();
        Assertions.assertEquals(PartitioningMode.ROUND_ROBIN, partitionStrategy.getPartitioningMode());
    }

    @Test
    public void testScaleUpStrategyBuilder() {
        PartitionStrategy partitionStrategy = PartitionStrategy.builder()
                .partitioningMode(PartitioningMode.SCALE_UP)
                .build();
        Assertions.assertEquals(PartitioningMode.SCALE_UP, partitionStrategy.getPartitioningMode());
    }

}
