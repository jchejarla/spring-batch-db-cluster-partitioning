# Installation

## 1. Add the dependency

Maven:

```xml
<dependency>
    <groupId>io.github.jchejarla</groupId>
    <artifactId>spring-batch-db-cluster-core</artifactId>
    <version><!-- the version shown in the selector above --></version>
</dependency>
```

## 2. Create the schema

This library adds three tables alongside the standard Spring Batch tables: `BATCH_NODES`,
`BATCH_JOB_COORDINATION`, and `BATCH_PARTITIONS`. DDL for PostgreSQL, MySQL, MariaDB, Oracle,
SQL Server, Db2, and H2 is bundled in the library under `schema/`.

- **Development:** let the framework create them on startup — set
  `spring.batch.cluster.initialize-schema=embedded` (default; creates them only on embedded databases
  such as H2). It runs after Spring Batch's own schema so the foreign keys resolve.
- **Production:** apply the bundled DDL with a managed migration tool (Flyway/Liquibase) or by hand.

See [Configuration](configuration.md) for `initialize-schema` and the Spring Batch core-schema
prerequisite.

## 3. Enable clustering

```yaml
spring:
  batch:
    cluster:
      enabled: true
      node-id-prefix: ${HOSTNAME:batch-node}   # optional; a unique suffix is added automatically
```

That's it — your existing job and step definitions are unchanged. Next, implement a
`ClusterAwarePartitioner` (see the [API reference](apidocs/index.html)) and wire it with the
`ClusterAwarePartitionHandler`.
