package dev.jchejarla.springbatch.clustering.actuate;

import dev.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import dev.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import dev.jchejarla.springbatch.clustering.mgmt.ClusterNodeInfo;
import dev.jchejarla.springbatch.clustering.mgmt.NodeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@ConditionalOnEnabledHealthIndicator("BatchClusterNode")
@ConditionalOnClusterEnabled
public class BatchClusterNodeHealthIndicator implements HealthIndicator {

    private final ClusterNodeInfo clusterNodeInfo;

    @Override
    public Health health() {
        Map<String, String> details = new HashMap<>();
        details.put("Node Id", clusterNodeInfo.getNodeId());
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
