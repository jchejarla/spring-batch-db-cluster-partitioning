package dev.jchejarla.springbatch.clustering.autoconfigure;

import dev.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "spring.batch.cluster")
@ConditionalOnClusterEnabled
public class BatchClusterProperties {
    private boolean enabled = false;
    private HostIdentifier hostIdentifier = HostIdentifier.HOST_NAME;
    private boolean tracingEnabled = false;
    private String nodeId;
    private long masterTaskStatusCheckInterval = 500; //Thread sleep time between each check, when checking partition tasks status
    private long pollForPartitionTasks=1000;
    private long pollingIntervalForOrphanedTasks=1000;
    private long workerNodeHeartBeatThreadInterval = 3000;
    private long workerNodeMarkUnreachableThreadInterval = 5000;
    private long workerNodeDeleteUnreachableThreadInterval = 5000;
    private long workerMarkUnreachableTimeDiff = 10000;
    private long workerDeleteUnreachableTimeDiff = 30000;

    public enum HostIdentifier {
        HOST_NAME,
        IP_ADDRESS
    }
}
