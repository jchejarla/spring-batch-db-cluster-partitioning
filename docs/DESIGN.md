# Design

How `spring-batch-db-cluster-partitioning` coordinates distributed Spring Batch partitioning through a relational database. This document explains the model and the rationale behind it; for API/configuration details see the Javadoc and `BatchClusterProperties`.

## Design philosophy

The relational database that Spring Batch already requires is treated as the **sole coordination plane**. Every piece of cluster state — node liveness, partition lifecycle, and the binding of a job to its master — lives in standard SQL tables. There is no message broker, no dedicated coordination service, and no deployment platform to operate. This keeps deployments lightweight and makes all coordination state transparent and queryable with ordinary SQL.

The target is small-to-medium clusters (roughly 2–20 nodes), where database throughput comfortably absorbs heartbeat and partition-tracking traffic.

## Coordination tables

Three tables augment Spring Batch's own schema:

- **`BATCH_NODES`** — the node registry: one row per node with status (`ACTIVE` / `UNREACHABLE`), heartbeat timestamp, and current load.
- **`BATCH_PARTITIONS`** — partition lifecycle: each partition's assigned node and status (`PENDING` → `CLAIMED` → `COMPLETED` / `FAILED`), plus whether it is transferable on failure.
- **`BATCH_JOB_COORDINATION`** — binds each job execution to its master node and manager step, with status (`CREATED` → `STARTED` → `COMPLETED`, or `ABANDONED` on recovery).

## Capacity-aware partitioning

Standard remote partitioning splits work *before* knowing how much capacity exists to run it: the grid size is chosen blind and the pieces are dispatched. Over-split and partitions queue; under-split and capacity sits idle.

Here the master first queries the live nodes (`getActiveNodes()`), and passes that count into the user's partitioner via `createDistributedPartitions(int availableNodeCount)`. The split is therefore sized to the cluster that actually exists at that instant.

## Decentralized, per-job master

There is no elected, standing master. **The node that launches a job becomes that job's master** for that execution: it splits the work, writes the coordination and partition rows, runs the completion/reassignment monitor loop, and finalizes the job. Two jobs launched on two different nodes have two different masters, concurrently.

A key consequence is **fault isolation**: because mastership is scoped to a single job execution, losing a master affects only that one job — never the cluster. There is no single coordinator to elect, and none to lose.

## Execution lifecycle

1. **Registration** — on startup each node inserts itself into `BATCH_NODES` and begins heartbeating.
2. **Partitioning (master)** — the master queries live nodes, the partitioner produces work units sized to that count, an assignment strategy maps them to nodes, and the partitions are written `PENDING`.
3. **Claim & execute (workers)** — every node polls `BATCH_PARTITIONS` for partitions assigned to it, transitions them `CLAIMED` transactionally, and runs the Spring Batch step (concurrently, up to `concurrencyLimitPerNode`), recording `COMPLETED` / `FAILED`.
4. **Completion (master)** — the master waits until no partitions remain `PENDING`/`CLAIMED`, then aggregates and finalizes the job.

## Partition assignment strategies

Pluggable via `PartitionAssignmentStrategy`: round-robin, fixed-node-count, and **least-loaded** — the last being load-aware, assigning each partition to the node with the lowest live load (tracked in `BATCH_NODES`), so work is steered away from nodes already busy with other jobs.

## Fault tolerance

### Node lifecycle (two-phase)

Every node runs the cleanup sweeps, so detection is itself decentralized:

1. **Phase 1 — mark unreachable**: a node whose heartbeat is older than `unreachableNodeThreshold` is flipped `ACTIVE → UNREACHABLE`.
2. **Phase 2 — remove**: an unreachable node older than the longer `nodeCleanupThreshold` is removed, and its transferable incomplete partitions become eligible for reassignment.

The two thresholds give a deliberate grace window: a brief GC pause or network blip marks a node unreachable (recoverable on the next heartbeat) without immediately removing it or stealing its work.

### Worker failure

The master's monitor reassigns orphaned partitions — those belonging to a removed node — to healthy nodes. Two deliberate properties:

- **Transferable-only**: only partitions the user marked transferable (`arePartitionsTransferableWhenNodeFailed()`) are moved. Non-transferable partitions (node-local state, non-idempotent side effects) are never reassigned — correctness over availability, by contract.
- **Self-fencing**: a node that has lost its own heartbeat stops claiming new work and aborts before executing a claimed partition. Combined with the transactional `CLAIMED` transition, this is what prevents a reassigned partition from being executed twice.

### Master failure

Today, a node detects a job whose master has left the cluster (a `BATCH_JOB_COORDINATION` row still `STARTED` whose master node is gone), **atomically claims it** so exactly one survivor acts, and marks the stranded job execution `FAILED` — making it cleanly restartable instead of hanging in `STARTED` forever. The atomic claim plus the node-removal grace window guard against acting on a master that is merely slow. The claim also takes ownership of the coordination row, so if the recovering node itself dies mid-recovery the row is re-detected and re-claimed by another node (recovery is idempotent).

Because this phase recovers by **fail-and-restart** rather than resume, a job is restarted even in the edge case where its master died after all partitions had completed but before finalizing — so **restarts must be idempotent**. (Resume-in-place is the roadmap item below.)

**Roadmap:** automatic *takeover* — a surviving node resuming the job (reassigning remaining work and finalizing it) rather than requiring a restart. The data model already supports this; the open work is split-brain-safe correctness.

## Supported databases

PostgreSQL, MySQL, MariaDB, Oracle, SQL Server, Db2, and H2, via per-database query providers (`DBSpecificQueryProvider`). Database-specific SQL is isolated there; the rest is standard SQL shared across all of them.

## Scope and limitations

- Tuned for ~2–20 nodes. For far larger clusters or sub-second coordination latency, a dedicated coordination service or broker-based architecture may fit better.
- The shared database is a dependency; production deployments should run it replicated with appropriate backup/failover.

## Related work

- **Spring Batch native remote partitioning** dispatches partitions over a message broker (Kafka/RabbitMQ/JMS) via Spring Integration. It has no built-in live-worker awareness and no automatic detection/reassignment when a worker dies after receiving a partition — the gaps this extension closes. The broker is not at fault; it was never meant to provide that coordination.
- **Spring Cloud Task `DeployerPartitionHandler`** is also broker-free and coordinates through the job repository, but it *provisions a worker per partition* through a deployment platform (Kubernetes, Cloud Foundry, …). This extension instead coordinates a standing cluster of peer JVM nodes that poll for work — no platform required — and sizes partitioning to the live node count.
- **DB-coordinated schedulers/runners** such as Quartz (JDBC clustering), db-scheduler, and JobRunr share the "coordinate through a database, fail over on node loss" idea, but address scheduling or general background-job execution rather than Spring Batch partitioned-step execution.
