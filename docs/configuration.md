# Configuration

All settings live under the `spring.batch.cluster.*` namespace and are bound by `BatchClusterProperties`.
Clustering is opt-in (`enabled=true`). Intervals and thresholds are in **milliseconds**; the defaults
suit small-to-medium clusters (~2–20 nodes).

| Property | Default | Description |
|---|---|---|
| `enabled` | `false` | Master switch. When `false`, no cluster components are activated. |
| `node-id-prefix` | host name | Optional readable prefix; the framework appends a unique suffix, so the node id is unique per JVM **and** per restart. |
| `host-identifier` | `HOST_NAME` | Register a node by `HOST_NAME` or `IP_ADDRESS`. |
| `initialize-schema` | `embedded` | Create the cluster tables on startup: `always` / `embedded` (embedded DBs only) / `never`. Prefer Flyway/Liquibase in production. |
| `concurrency-limit-per-node` | `10` | Max partition steps a node runs concurrently. |
| `heartbeat-interval` | `3000` | How often a node updates its heartbeat. |
| `unreachable-node-threshold` | `10000` | Heartbeat age after which a node is marked `UNREACHABLE` (failover phase 1). |
| `node-cleanup-threshold` | `30000` | Heartbeat age after which an unreachable node is removed and its transferable partitions reassigned (phase 2). |
| `unreachable-node-thread-interval` | `5000` | How often the phase-1 sweep runs. |
| `node-cleanup-thread-interval` | `5000` | How often the phase-2 sweep runs. |
| `task-polling-interval` | `1000` | How often a worker polls for partitions assigned to it. |
| `completed-tasks-cleanup-polling-interval` | `5000` | How often a worker prunes records of completed tasks. |
| `orphaned-tasks-polling-interval` | `1000` | How often the master scans for orphaned partitions to reassign. |
| `master-task-status-check-interval` | `500` | Master-side sleep between partition-completion checks. |
| `orphaned-master-scan-interval` | `10000` | How often each node scans for jobs whose master node was lost, so a stranded execution can be abandoned and made restartable. |
| `capture-phase-timings` | `false` | When `true`, records master-side coordination phase timestamps to the append-only `BATCH_JOB_PHASE_EVENTS` table (database clock). Pair with a retention policy; the table grows over time. |

!!! note "Spring Batch core schema is a prerequisite"
    These tables have foreign keys to the standard Spring Batch tables, so the Spring Batch schema must
    exist first (`spring.batch.jdbc.initialize-schema`, or a migration tool). The cluster auto-DDL runs
    after it.

!!! warning "Actuator exposure"
    The `/actuator/batch-cluster` endpoints expose node and partition detail. Restrict their exposure
    and protect them with authentication; do not expose them unauthenticated on a public interface.

The legacy `node-id` property has been removed (ids are generated automatically); a leftover value is
ignored with a warning. Use `node-id-prefix` instead.
