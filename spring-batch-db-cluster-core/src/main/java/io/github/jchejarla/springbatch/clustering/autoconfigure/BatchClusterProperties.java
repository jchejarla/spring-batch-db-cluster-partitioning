package io.github.jchejarla.springbatch.clustering.autoconfigure;

import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for database-coordinated Spring Batch clustering, bound from the
 * {@code spring.batch.cluster.*} namespace.
 *
 * <p>Clustering is opt-in: set {@code spring.batch.cluster.enabled=true} to activate it. All other
 * properties are intervals and thresholds (in milliseconds) that tune heartbeating, node
 * liveness/cleanup, worker polling, and recovery. The defaults suit small-to-medium clusters
 * (roughly 2&ndash;20 nodes); tune them to trade coordination-database load against failure-detection
 * latency.</p>
 *
 * @author Janardhan Chejarla
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "spring.batch.cluster")
@ConditionalOnClusterEnabled
public class BatchClusterProperties {

    /** Master switch for clustering. When {@code false}, none of the cluster components are activated. */
    private boolean enabled = false;

    /** Whether a node registers itself in {@code BATCH_NODES} by host name or IP address. */
    private HostIdentifier hostIdentifier = HostIdentifier.HOST_NAME;

    /** When {@code true}, emits verbose timing/diagnostic logs for heartbeats and polling. Keep off in production. */
    private boolean tracingEnabled = false;

    /**
     * Unique id for this node within the cluster. Must be distinct per JVM; if unset, supply one per
     * instance (e.g. via an environment variable). Reusing an id across live JVMs breaks coordination.
     */
    private String nodeId;

    /** Maximum number of partition steps this node executes concurrently. */
    private int concurrencyLimitPerNode = 10;

    /** How often this node updates its heartbeat (and refreshes its view of the cluster). */
    private long heartbeatInterval = 3000;

    /** How often the phase-1 sweep runs that marks stale nodes {@code UNREACHABLE}. */
    private long unreachableNodeThreadInterval = 5000;

    /** How often the phase-2 sweep runs that removes long-unreachable nodes from the registry. */
    private long nodeCleanupThreadInterval = 5000;

    /** Heartbeat age after which a node is marked {@code UNREACHABLE} (phase 1 of failover). */
    private long unreachableNodeThreshold = 10000;

    /** Heartbeat age after which an unreachable node is removed and its transferable partitions reassigned (phase 2). */
    private long nodeCleanupThreshold = 30000;

    /** How often a worker polls {@code BATCH_PARTITIONS} for partitions assigned to it. */
    private long taskPollingInterval =1000;

    /** How often a worker prunes its records of completed partition tasks. */
    private long completedTasksCleanupPollingInterval =5000;

    /** How often the master scans for orphaned partitions (assigned to a lost node) to reassign. */
    private long orphanedTasksPollingInterval =1000;

    /** Thread sleep time between each master-side check of overall partition completion. */
    private long masterTaskStatusCheckInterval = 500;

    /**
     * How often each node scans for jobs whose master node has left the cluster, so the stranded
     * (and otherwise permanently {@code STARTED}) job execution can be abandoned and made restartable.
     */
    private long orphanedMasterScanInterval = 10000;

    public enum HostIdentifier {
        HOST_NAME,
        IP_ADDRESS
    }
}
