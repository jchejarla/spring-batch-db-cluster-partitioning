package io.github.jchejarla.springbatch.clustering.actuate;

import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeInfo;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNodeManager;
import io.github.jchejarla.springbatch.clustering.mgmt.NodeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
