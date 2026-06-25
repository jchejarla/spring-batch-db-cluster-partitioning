package io.github.jchejarla.springbatch.clustering.autoconfigure;

import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.UUID;

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
     * Optional prefix for this node's auto-generated id. Defaults to the machine host name when unset.
     * The actual node id is always {@code <prefix>-<random-uuid>}, generated once at startup, so it is
     * guaranteed unique per JVM and per restart with no manual configuration.
     */
    private String nodeIdPrefix;

    /**
     * The resolved, cluster-unique id for this node, generated at startup as
     * {@code <node-id-prefix>-<uuid>}. Read-only: it is produced by the framework, not configured.
     * The host name is recorded separately in {@code BATCH_NODES.HOST_IDENTIFIER} for observability.
     */
    @Setter(AccessLevel.NONE)
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

    /**
     * Generates the cluster-unique node id once, after configuration binding, as
     * {@code <node-id-prefix or host name>-<uuid>}.
     */
    @PostConstruct
    void resolveNodeId() {
        String prefix = StringUtils.hasText(nodeIdPrefix) ? nodeIdPrefix.trim() : defaultHostName();
        this.nodeId = prefix + "-" + UUID.randomUUID();
    }

    private String defaultHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "node";
        }
    }

    public enum HostIdentifier {
        HOST_NAME,
        IP_ADDRESS
    }
}
