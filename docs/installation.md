# Installation

!!! info "Upgrading from 2.x?"
    3.0.0 is a platform release (Spring Boot 4 / Spring Batch 6 / Java 21). See the
    [Migration Guide](migration.md).

## Requirements

- **Java 21+**
- **Spring Boot 4.1+** / **Spring Batch 6** (the 2.x line targets Spring Boot 3)
- A relational database (PostgreSQL, MySQL, MariaDB, Oracle, SQL Server, Db2, or H2 for development)

## 1. Add the dependency

Maven:

```xml
<dependency>
    <groupId>io.github.jchejarla</groupId>
    <artifactId>spring-batch-db-cluster-core</artifactId>
    <version><!-- the version shown in the selector above --></version>
</dependency>
```

!!! note "Testing a SNAPSHOT build?"
    SNAPSHOTs are pre-release, not official versions. To consume one, add the snapshot repository:

    ```xml
    <repositories>
      <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots><enabled>true</enabled></snapshots>
        <releases><enabled>false</enabled></releases>
      </repository>
    </repositories>
    ```

## 2. Use a JDBC JobRepository

Spring Batch 6 defaults to an in-memory `ResourcelessJobRepository` that persists nothing. A cluster
coordinates through **shared, persisted** job metadata, so a JDBC-backed repository is required. Enable
it on a configuration class:

```java
@SpringBootApplication
@EnableBatchProcessing
@EnableJdbcJobRepository   // dataSourceRef defaults to "dataSource"
public class MyApplication { }
```

Both annotations are needed — `@EnableJdbcJobRepository` is inert without `@EnableBatchProcessing`. If
clustering is enabled on the resourceless repository, startup fails fast with an actionable message.

## 3. Create the schema

Two layers of tables must exist:

- **Spring Batch core tables** (`BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION`, …). Spring Boot 4 no
  longer auto-creates these, so use Flyway/Liquibase, or `spring.sql.init`:

    ```yaml
    spring:
      sql:
        init:
          mode: always
          schema-locations: classpath:org/springframework/batch/core/schema-@@platform@@.sql
    ```

- **Cluster tables** — `BATCH_NODES`, `BATCH_JOB_COORDINATION`, `BATCH_PARTITIONS`, and
  `BATCH_JOB_PHASE_EVENTS`. DDL for all supported databases is bundled under `schema/`.
    - **Development:** let the framework create them — `spring.batch.cluster.initialize-schema=embedded`
      (default; embedded databases such as H2 only). It runs after the Spring Batch schema (via
      `@DependsOnDatabaseInitialization`) so the foreign keys resolve.
    - **Production:** apply the bundled DDL with your migration tool or by hand.

See [Configuration](configuration.md) for `initialize-schema` and the schema prerequisite.

## 4. Enable clustering

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
