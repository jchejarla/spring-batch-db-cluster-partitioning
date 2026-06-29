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
import io.github.jchejarla.springbatch.clustering.mgmt.NodeLoad;
import io.github.jchejarla.springbatch.clustering.mgmt.NodeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Spring Boot {@link HealthIndicator} reporting this node's own cluster health — its current status
 * and most recent heartbeat.
 */
@RequiredArgsConstructor
@ConditionalOnEnabledHealthIndicator("BatchClusterNode")
@ConditionalOnClusterEnabled
public class BatchClusterNodeHealthIndicator implements HealthIndicator {

    private final ClusterNodeInfo clusterNodeInfo;

    @Override
    public Health health() {
        Map<String, String> details = new HashMap<>();
        details.put("Node Id", clusterNodeInfo.getNodeId());
        details.put("Current Load (number of live tasks)", String.valueOf(NodeLoad.INST.getCurrentLoad()));
        if(Objects.nonNull(clusterNodeInfo.getStartTime())) {
            details.put("Start Time", clusterNodeInfo.getStartTime().toString());
        }
        if(Objects.nonNull(clusterNodeInfo.getLastHeartbeatTime())) {
            details.put("Last Heartbeat Update Time", clusterNodeInfo.getLastHeartbeatTime().toString());
        }
        if(Objects.nonNull(clusterNodeInfo.getNodeStatus())) {
            details.put("Node Status", clusterNodeInfo.getNodeStatus().name());
        }
        details.put("Current Load", String.valueOf(clusterNodeInfo.getCurrentLoad()));
        if(clusterNodeInfo.getNodeStatus() == NodeStatus.ACTIVE) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }
}
