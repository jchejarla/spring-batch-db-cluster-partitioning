# Spring Batch DB Cluster Partitioning

Database-coordinated distributed partitioning for Spring Batch — scalable, fault-tolerant partitioned
job execution across JVM nodes, coordinated entirely through the relational database you already
operate. No additional messaging or coordination infrastructure to deploy or monitor.

!!! info "Versioned documentation"
    Use the version selector (top of the page) to view the documentation that matches the release you
    are running. Each release publishes its own docs and [API reference](apidocs/index.html).

## Start here

- **[Installation](installation.md)** — add the dependency, create the schema, enable clustering.
- **[Configuration](configuration.md)** — the full `spring.batch.cluster.*` reference.
- **[Design](DESIGN.md)** — architecture, the decentralized-master model, and fault tolerance.
- **[FAQ](faq.md)** — common questions, including how this differs from broker-based partitioning.
- **[API (Javadoc)](apidocs/index.html)** — the published API for this version.

## What it is

The node that launches a job becomes that job's master: it queries the live worker count, sizes the
partitioning to the cluster that actually exists, distributes work through the database, and monitors
completion. Workers claim partitions transactionally and execute the standard Spring Batch step.
Failover and recovery are handled through the same database — no message broker required.
