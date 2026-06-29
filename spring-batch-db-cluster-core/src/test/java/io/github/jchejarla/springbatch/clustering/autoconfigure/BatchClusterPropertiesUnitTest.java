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
package io.github.jchejarla.springbatch.clustering.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BatchClusterPropertiesUnitTest {

    @Test
    public void testNodeIdUsesConfiguredPrefix() {
        BatchClusterProperties props = new BatchClusterProperties();
        props.setNodeIdPrefix("worker");
        props.resolveNodeId();
        assertTrue(props.getNodeId().startsWith("worker-"));
        assertTrue(props.getNodeId().length() > "worker-".length(), "a unique suffix should be appended");
    }

    @Test
    public void testNodeIdDefaultsToHostNameWhenNoPrefix() {
        BatchClusterProperties props = new BatchClusterProperties();
        props.resolveNodeId();
        assertNotNull(props.getNodeId());
        assertTrue(props.getNodeId().contains("-"), "id should be <prefix>-<uuid>");
    }

    @Test
    public void testNodeIdIsUniquePerResolution() {
        BatchClusterProperties a = new BatchClusterProperties();
        a.setNodeIdPrefix("worker");
        a.resolveNodeId();
        BatchClusterProperties b = new BatchClusterProperties();
        b.setNodeIdPrefix("worker");
        b.resolveNodeId();
        assertNotEquals(a.getNodeId(), b.getNodeId());
    }
}
