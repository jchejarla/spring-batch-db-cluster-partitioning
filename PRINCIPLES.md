# Design Principles

`spring-batch-db-cluster-partitioning` turns a single relational database into the coordination plane for
distributed, fault-tolerant Spring Batch partitioning across many JVM nodes — no message broker and no
separate coordination service required. These are the principles that shape the design. The approach is
described and peer-reviewed in the *Journal of Open Source Software* ([DOI: 10.21105/joss.09460](https://doi.org/10.21105/joss.09460)).

## 1. The database is the only coordination plane

No message broker, no ZooKeeper/etcd, no separate control service. Everything a cluster needs — node
membership, work distribution, completion detection, and failover — flows through the same relational
database Spring Batch already requires. The database is already a trusted, transactional, operated part of
the stack; reusing it removes an entire class of infrastructure you would otherwise deploy, secure, and
monitor.

## 2. Canonical Spring Batch remote partitioning, with the broker removed

This is not a new execution model. Partitions are ordinary Spring Batch `StepExecution`s persisted in the
shared job repository; a worker on another JVM loads a partition by id and runs the standard step —
exactly as Spring Batch's own message-based remote partitioning does. The only thing replaced is the
*trigger transport*: instead of a step-execution request delivered over Kafka/RabbitMQ/JMS, workers
discover their assigned partitions by querying the database. Same durability model, same worker code path,
one fewer moving part to run.

## 3. Decentralized, per-job master

There is no standing leader and no leader-election protocol. Whichever node launches a job becomes the
master *for that job*; different jobs run under different masters at the same time. There is no
control-plane single point of failure to protect.

## 4. Coordination latency is bounded by the poll interval — by design

With no broker to push notifications, nodes learn of work by polling the database on an interval. This is
a deliberate trade, not an oversight: you accept up-to-one-interval pickup latency in exchange for zero
additional infrastructure. The interval is a single tunable knob, so each deployment chooses its own point
on the latency-versus-load curve.

## 5. Every state transition is transactional — work is never double-run or lost

A partition moves `PENDING → CLAIMED → COMPLETED/FAILED` through transactional claims, so no two nodes can
execute the same partition and no partition silently disappears. Two safety contracts reinforce this:

- **`is_transferable`** — a partition marked non-transferable (node-local state, non-idempotent side
  effects) is never re-executed on another node. If its owner is lost, the job fails cleanly rather than
  risk a double run.
- **Heartbeat-loss fencing** — a node that loses its heartbeat cancels its in-flight partitions and stops
  working, leaving them for the master to recover. A fenced node cannot keep mutating shared state.

## 6. Two-phase failure detection, and recovery through the same database

Node failure is handled in two stages: missed heartbeats mark a node `UNREACHABLE`; a longer threshold
removes it and redistributes its transferable partitions across the healthy nodes. Master failure is
handled too — a surviving node detects a job whose master has left the cluster, atomically claims it, and
makes the stranded execution restartable instead of leaving it hung. All of this is coordinated through
the database; no external coordinator is consulted.

## 7. Work is sized to the cluster that actually exists

Before partitioning, the master asks the database which nodes are currently alive and passes that live
count to the partitioner, so work is shaped to real capacity rather than a static assumption. Assignment
is pluggable and load-aware — round-robin, fixed-node-count, or least-loaded (steering partitions toward
the least-busy nodes).

## 8. Portable across databases

Coordination is expressed in portable SQL, with database-specific statements isolated behind per-dialect
providers. PostgreSQL, MySQL, MariaDB, Oracle, SQL Server, Db2, and H2 are supported, and adding a dialect
is a contained change.

## 9. Observable through the same plane it coordinates on

Because coordination state lives in the database, it is directly inspectable. The library exposes
job-centric views and actuator endpoints — per job: the master node, partition count, status distribution,
and where each partition is running — and can capture the master-side coordination phases to an
append-only event log for latency analysis.

---

Target scale is small-to-medium clusters (roughly 2–20 nodes), where treating the database as the
coordination plane is both sufficient and operationally simplest. See the
[documentation](https://jchejarla.github.io/spring-batch-db-cluster-partitioning/docs/) for configuration
and the [JOSS paper](https://doi.org/10.21105/joss.09460) for the peer-reviewed description.
