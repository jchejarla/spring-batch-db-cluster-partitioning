package io.github.jchejarla.springbatch.clustering.autoconfigure;

import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
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
    private int concurrencyLimitPerNode = 10;
    private long heartbeatInterval = 3000;
    private long unreachableNodeThreadInterval = 5000;
    private long nodeCleanupThreadInterval = 5000;
    private long unreachableNodeThreshold = 10000;
    private long nodeCleanupThreshold = 30000;
    private long taskPollingInterval =1000;
    private long completedTasksCleanupPollingInterval =5000;
    private long orphanedTasksPollingInterval =1000;
    private long masterTaskStatusCheckInterval = 500; //Thread sleep time between each check, when checking partition tasks status

    public enum HostIdentifier {
        HOST_NAME,
        IP_ADDRESS
    }
}
