package io.github.jchejarla.springbatch.clustering.mgmt;

import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.core.DatabaseBackedClusterService;
import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RequiredArgsConstructor
@ConditionalOnClusterEnabled
public class ClusterNodeManager {

    private final DatabaseBackedClusterService databaseBackedClusterService;
    private final BatchClusterProperties batchClusterProperties;
    private final TaskScheduler clusterMonitoringScheduler;
    private final ClusterNodeInfo clusterNodeInfo;
    private final ClusterNodeStatusChangeConditionNotifier clusterNodeStatusChangeConditionNotifier;
    @Getter
    private final List<ClusterNodeInfo> currentNodes = new CopyOnWriteArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("Registering the node with id {}", batchClusterProperties.getNodeId());
        long start = System.currentTimeMillis();
        clusterNodeInfo.setStartTime(new Date());
        int rowsUpdated = databaseBackedClusterService.registerNode();
        if(rowsUpdated == 0) {
            log.error("Application failed to register the node with id {}", batchClusterProperties.getNodeId());
            throw new BatchConfigurationException("Application failed to register the node with id "+batchClusterProperties.getNodeId());
        }
        clusterNodeInfo.setNodeStatus(NodeStatus.ACTIVE);
        log.info("Application registered the node with id {}, and it took {} milli seconds", batchClusterProperties.getNodeId(), (System.currentTimeMillis() - start));
        clusterMonitoringScheduler.scheduleAtFixedRate(this::updateHeartbeat, Duration.ofMillis(batchClusterProperties.getHeartbeatInterval()));
        clusterMonitoringScheduler.scheduleAtFixedRate(this::updateCurrentActiveNodes, Duration.ofMillis(batchClusterProperties.getHeartbeatInterval()));
        clusterMonitoringScheduler.scheduleAtFixedRate(this::markNodesUnreachable, Duration.ofMillis(batchClusterProperties.getUnreachableNodeThreadInterval()));
        clusterMonitoringScheduler.scheduleAtFixedRate(this::removeNodesUnreachable, Duration.ofMillis(batchClusterProperties.getNodeCleanupThreadInterval()));
    }

    protected void updateHeartbeat() {
        try {
            long start = System.currentTimeMillis();
            int rowsUpdated = databaseBackedClusterService.updateNodeHeartbeat();
            if (rowsUpdated == 0) {
                log.error("Application failed to update the heartbeat for node id {}, trying to one more time ", batchClusterProperties.getNodeId());
                 rowsUpdated = databaseBackedClusterService.registerNode();
                 if(rowsUpdated == 1) {
                     clusterNodeInfo.setNodeStatus(NodeStatus.ACTIVE);
                     clusterNodeInfo.setLastHeartbeatTime(new Date());
                     log.info("Application re-registered the node with id {}, and it took {} milli seconds", batchClusterProperties.getNodeId(), (System.currentTimeMillis() - start));
                 } else {
                     log.error("Re-attempt to register the node is failed for node id {}", batchClusterProperties.getNodeId());
                     clusterNodeInfo.setNodeStatus(NodeStatus.UNREACHABLE);
                     clusterNodeStatusChangeConditionNotifier.onClusterNodeHeartbeatFail();
                 }
            } else {
                clusterNodeInfo.setNodeStatus(NodeStatus.ACTIVE);
                clusterNodeInfo.setLastHeartbeatTime(new Date());
                if(batchClusterProperties.isTracingEnabled()) {
                    log.info("Application updated heartbeat for node id {}, and it took {} milli seconds", batchClusterProperties.getNodeId(), (System.currentTimeMillis() - start));
                }
            }
        } catch(Exception e) {
            log.error("Exception occurred while updating heart beat for node id : {}", batchClusterProperties.getNodeId(), e);
            clusterNodeInfo.setNodeStatus(NodeStatus.UNREACHABLE);
            clusterNodeStatusChangeConditionNotifier.onClusterNodeHeartbeatFail();
        }
    }

    protected void markNodesUnreachable() {
        try {
            long start = System.currentTimeMillis();
            int rowsUpdated = databaseBackedClusterService.markNodesUnreachable();
            if (rowsUpdated > 0 || batchClusterProperties.isTracingEnabled()) {
                log.info("PHASE1 of Nodes removal from Application have marked {} nodes that are not reachable, i.e. nodes status was not updated in last {} milli seconds, marking process took {} milliseconds", rowsUpdated, batchClusterProperties.getUnreachableNodeThreshold(), (System.currentTimeMillis() - start));
            }
        } catch(Exception e) {
            log.error("Exception occurred while marking node id : {} unreachable", batchClusterProperties.getNodeId(), e);
        }
    }

    protected void removeNodesUnreachable() {
        try {
            long start = System.currentTimeMillis();
            int rowsUpdated = databaseBackedClusterService.deleteNodesUnreachable();
            if (rowsUpdated > 0 || batchClusterProperties.isTracingEnabled()) {
                log.info("PHASE2 of Nodes removal from Application have removed {} nodes that are not reachable, i.e. nodes status was not updated in last {} milli seconds, delete process took {} milliseconds", rowsUpdated, batchClusterProperties.getNodeCleanupThreshold(), (System.currentTimeMillis() - start));
            }
        } catch(Exception e) {
            log.error("Exception occurred while removing node id : {} from cluster", batchClusterProperties.getNodeId(), e);
        }
    }

    protected void updateCurrentActiveNodes() {
        try {
            List<ClusterNodeInfo> clusterNodeInfos = databaseBackedClusterService.getNodesInCluster();
            currentNodes.clear();
            currentNodes.addAll(clusterNodeInfos);
        } catch(Exception e) {
            log.error("Exception occurred while fetching nodes info in the cluster");
        }
    }

}
