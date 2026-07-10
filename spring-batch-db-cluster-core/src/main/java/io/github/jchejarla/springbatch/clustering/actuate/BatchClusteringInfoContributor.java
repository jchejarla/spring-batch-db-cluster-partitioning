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
package io.github.jchejarla.springbatch.clustering.actuate;

import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import java.util.Map;

/**
 * Contributes the active clustering configuration (intervals and thresholds) to the
 * {@code /actuator/info} endpoint.
 */
@RequiredArgsConstructor
@ConditionalOnClusterEnabled
public class BatchClusteringInfoContributor implements InfoContributor {

    // Read from the jar manifest (Implementation-Version) so /actuator/info always reports the real
    // artifact version instead of a hardcoded string that silently drifts across releases. Falls back
    // to "unknown" when running without a manifest (e.g. from an IDE / exploded classes).
    private static final String VERSION = resolveVersion();

    private static String resolveVersion() {
        String version = BatchClusteringInfoContributor.class.getPackage().getImplementationVersion();
        return version != null ? version : "unknown";
    }

    private final BatchClusterProperties batchClusterProperties;

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("Batch Clustering Properties",
                Map.of("Batch clustering version", VERSION,
                        "Node heartbeat interval (milli seconds)", batchClusterProperties.getHeartbeatInterval(),
                        "Node unreachable marking threshold (milli seconds)", batchClusterProperties.getUnreachableNodeThreshold(),
                        "Node delete threshold (milli seconds)", batchClusterProperties.getNodeCleanupThreshold(),
                        "Check for tasks polling interval (milli seconds)", batchClusterProperties.getTaskPollingInterval(),
                        "Master task status check interval (milli seconds)", batchClusterProperties.getMasterTaskStatusCheckInterval(),
                        "Concurrency limit per node", batchClusterProperties.getConcurrencyLimitPerNode(),
                        "Check for orphaned tasks interval (milli seconds)", batchClusterProperties.getOrphanedTasksPollingInterval(),
                        "Node unreachable marking-thread interval (milli seconds)", batchClusterProperties.getUnreachableNodeThreadInterval(),
                        "Node cleanup-thread interval (milli seconds)", batchClusterProperties.getNodeCleanupThreadInterval()));
        }
}
