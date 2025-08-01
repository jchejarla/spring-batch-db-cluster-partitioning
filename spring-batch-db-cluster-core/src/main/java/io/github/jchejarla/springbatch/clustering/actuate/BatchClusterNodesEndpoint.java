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
