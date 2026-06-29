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

import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeInfo;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeManager;
import io.github.jchejarla.springbatch.clustering.mgmt.NodeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot {@link HealthIndicator} reporting overall batch-cluster health (such as node liveness)
 * under {@code /actuator/health}.
 */
@RequiredArgsConstructor
@ConditionalOnEnabledHealthIndicator("BatchCluster")
@ConditionalOnClusterEnabled
public class BatchClusterHealthIndicator implements HealthIndicator {

    private final ClusterNodeManager clusterNodeManager;

    @Override
    public Health health() {
        Map<String, String> details = new HashMap<>();
        List<ClusterNodeInfo> allNodesInCluster = clusterNodeManager.getCurrentNodes();
        details.put("Total Nodes in Cluster", ""+allNodesInCluster.size());
        long activeNodes = allNodesInCluster.stream().filter(clusterNodeInfo -> clusterNodeInfo.getNodeStatus() == NodeStatus.ACTIVE).count();
        details.put("Total Active Nodes", ""+activeNodes);
        if(!allNodesInCluster.isEmpty()) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }
}
