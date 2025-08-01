package io.github.jchejarla.springbatch.clustering.actuate;

import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import java.util.Map;

@RequiredArgsConstructor
@ConditionalOnClusterEnabled
public class BatchClusteringInfoContributor implements InfoContributor {

    private static final String VERSION = "2.0.0";

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
