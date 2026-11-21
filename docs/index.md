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

## Publications

- **Journal of Open Source Software (peer-reviewed):** *spring-batch-db-cluster-partitioning: Database-driven
  clustering with heartbeats and failover for Spring Batch.* JOSS 11(122), 9460, 2026.
  [doi:10.21105/joss.09460](https://doi.org/10.21105/joss.09460)
- **InfoQ:** [*Distributed Spring Batch without a Message Broker*](https://www.infoq.com/articles/spring-batch-without-broker/)
  (October 2026) — the brokerless design: coordinating partitioning, claiming, and failover through the
  shared database.

To cite this project, use the JOSS paper (BibTeX and `CITATION.cff` are in the
[repository](https://github.com/jchejarla/spring-batch-db-cluster-partitioning/blob/main/CITATION.cff)).
