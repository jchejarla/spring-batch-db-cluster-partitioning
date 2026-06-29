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
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Actuator endpoint exposed at {@code /actuator/batch-cluster} that lists the cluster's nodes and,
 * for a given node, its partition assignments and their status.
 */
@RequiredArgsConstructor
@ConditionalOnClusterEnabled
@Endpoint(id = "batch-cluster")
public class BatchClusterNodesEndpoint {

    private final ClusterNodeManager clusterNodeManager;

    @ReadOperation
    public Map<String, Object> clusterNodes() {
        List<ClusterNodeInfo> nodes = clusterNodeManager.getCurrentNodes();
        return Map.of(
                "totalNodes", nodes.size(),
                "nodes", nodes.stream()
                        .map(node -> Map.of(
                                "Node Id", node.getNodeId(),
                                "Host", node.getHostIdentifier(),
                                "Started At", node.getStartTime(),
                                "Status", node.getNodeStatus(),
                                "Last Heartbeat", node.getLastHeartbeatTime(),
                                "Current Load (# of tasks)", node.getCurrentLoad()

                        ))
                        .collect(Collectors.toList())
        );
    }


    @ReadOperation
    public ClusterNodeInfo nodeDetails(@Selector String nodeId) {
        return clusterNodeManager.getCurrentNodes().stream()
                .filter(node -> node.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }


}
