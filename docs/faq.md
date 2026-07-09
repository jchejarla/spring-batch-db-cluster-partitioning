# FAQ

### Why coordinate through the database instead of a message broker like Kafka or RabbitMQ?

The brokers aren't the problem — they deliver messages reliably. The gap is in how Spring Batch's
standard remote partitioning *uses* them: as a fire-and-forget dispatch channel, with no way to know
how many workers are alive before partitioning and no way to detect a worker that died after receiving
a partition. This extension adds that coordination layer — proactive node awareness, transactional
partition lifecycle tracking, and heartbeat-based failover — using the database Spring Batch already
requires, so there's no extra infrastructure to operate.

### How is this different from Spring Cloud Task's `DeployerPartitionHandler`?

That approach is also broker-free, but it provisions a worker per partition via a deployment platform
(Kubernetes, Cloud Foundry, …). This extension coordinates a **standing cluster of peer JVM nodes**
that poll for work — no deployment platform required — and queries how many nodes are live before
partitioning, sizing the workload to the cluster you already have.

### Does it work with Spring Batch 6's `ResourcelessJobRepository`? Do I need a JDBC job repository?

Every node must share one persistent, JDBC-backed `JobRepository`. The `ResourcelessJobRepository`
that Spring Batch 6 makes the default will not work — and that is not a limitation peculiar to this
extension.

Resourceless keeps job metadata in a single JVM's heap and persists nothing. Spring Batch's own
documentation scopes it to jobs "where … the execution context is not involved in any way (like …
partitioned steps where partitions meta-data is shared between the manager and workers through the
execution context)" — it *explicitly excludes* partitioning, and it is single-JVM and not thread-safe.
Distributed partitioning is exactly that excluded case: a partition's slice of work travels in the
execution context from the master to whichever node runs it, and the results travel back to be
aggregated, so the metadata store has to be shared and durable. Spring Batch's *own* remote
partitioning imposes the same requirement — its workers load each partition's `StepExecution` from the
shared repository; the broker only carries a pointer. The line is Spring Batch's own: a throwaway
single-JVM job needs no repository; a partitioned or distributed job needs a shared persistent one.
This extension sits on that line and asks for nothing more than the shared database — no broker on top.
Configure it with `@EnableBatchProcessing` + `@EnableJdbcJobRepository` (see the Migration guide, which
also describes the startup fail-fast guard).

### Do I have to launch jobs synchronously or asynchronously?

Your choice — job definition and launching stay standard Spring Batch. To avoid blocking an HTTP
request thread for a job's duration, configure an asynchronous `JobLauncher` and poll execution status.
That's a native Spring Batch capability; nothing extra here.

### What happens when a worker node dies mid-job?

Its heartbeat stops, so it is marked unreachable and then removed. Its incomplete partitions — if
marked transferable — are reassigned to healthy nodes. Non-transferable partitions are never moved (a
deliberate safety contract for work with node-local state or non-idempotent side effects).

### What happens when the master node dies?

Because mastership is per-job-execution, only that one job is affected. A surviving node detects the
lost master and marks the stranded job execution failed so it becomes cleanly restartable. Recovery is
fail-and-restart (not resume), so **restarts must be idempotent**; automatic takeover is on the roadmap.

### Which databases are supported?

PostgreSQL, MySQL, MariaDB, Oracle, SQL Server, Db2, and H2, via per-database query providers. H2
(file mode) is handy for local multi-node demos; the others for production.

### How large a cluster does this target?

Small-to-medium clusters — roughly 2–20 nodes — where the database comfortably handles heartbeat and
partition-tracking traffic. For far larger clusters or sub-second coordination needs, a dedicated
coordination service or broker-based architecture may fit better.
