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

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PartitionTransferablePropUnitTest extends BaseUnitTest {

    @Test
    public void testPartitionTransferableProp() {
        PartitionTransferableProp partitionTransferableProp = PartitionTransferableProp.YES;
        assertEquals(ClusterPartitioningConstants.IS_TRANSFERABLE, partitionTransferableProp.key);
        assertEquals(Boolean.TRUE, partitionTransferableProp.val);
        partitionTransferableProp = PartitionTransferableProp.NO;
        assertEquals(ClusterPartitioningConstants.IS_TRANSFERABLE, partitionTransferableProp.key);
        assertEquals(Boolean.FALSE, partitionTransferableProp.val);
    }
}
