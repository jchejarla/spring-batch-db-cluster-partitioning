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

The core artifact pulls in Spring Boot's web, batch, jdbc, and actuator starters transitively, so you
don't add those. It does **not** bring a database driver — add the one for your database yourself:

```xml
<!-- H2 for the zero-setup quick start (use runtime scope) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- ...or com.mysql:mysql-connector-j / org.postgresql:postgresql / etc. for a server DB -->
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

Two layers of tables must exist: the **Spring Batch core tables** and this library's **cluster tables**
(`BATCH_NODES`, `BATCH_JOB_COORDINATION`, `BATCH_PARTITIONS`, `BATCH_JOB_PHASE_EVENTS`). Spring Boot 4 no
longer auto-creates the Batch tables, so create both explicitly. The simplest portable way is
`spring.sql.init`, pointing at the DDL that ships for your database — pick the matching suffix (`-h2`,
`-postgresql`, `-mysql`, `-mariadb`, `-oracle`, `-sqlserver`, `-db2`):

```yaml
spring:
  sql:
    init:
      mode: always
      continue-on-error: true   # tolerate "already exists" across restarts and peer nodes
      schema-locations:
        - classpath:org/springframework/batch/core/schema-h2.sql   # 1. Spring Batch core tables
        - classpath:schema/schema-h2.sql                            # 2. this library's cluster tables
```

Order matters — the cluster tables have foreign keys to the Spring Batch tables, so the core schema
must be listed **first**. (For production, prefer a migration tool — Flyway/Liquibase — with
`spring.batch.cluster.initialize-schema=never`.)

!!! warning "Don't rely on `initialize-schema=embedded` for the H2 file demo"
    `spring.batch.cluster.initialize-schema` defaults to `embedded`, which auto-creates the cluster
    tables **only for genuinely in-memory databases**. It does **not** fire for **file-mode H2** (the
    multi-node demo path) or any server database — for those you must create the cluster tables yourself
    (via `spring.sql.init` as above, or a migration tool), and set `initialize-schema: never`.

See [Configuration](configuration.md) for the full `initialize-schema` semantics.

## 4. Enable clustering

```yaml
spring:
  batch:
    cluster:
      enabled: true
      node-id-prefix: ${HOSTNAME:batch-node}   # optional; a unique suffix is added automatically
```

## 5. A complete, runnable config (zero-setup H2, multi-node)

Putting it together — a full `application.yml` that forms a real cluster from multiple JVMs on one
machine using **file-mode H2** (no external database). Copy this, then start two instances on different
ports (`--server.port=8081`, `--server.port=8082`); they share the H2 file and form a cluster.

```yaml
spring:
  datasource:
    # AUTO_SERVER lets multiple JVMs share one H2 file — the first becomes an embedded server for the
    # rest. Swap this for a Postgres/MySQL/... datasource in production.
    url: jdbc:h2:file:./build/cluster-demo;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: always
      continue-on-error: true
      schema-locations:
        - classpath:org/springframework/batch/core/schema-h2.sql
        - classpath:schema/schema-h2.sql
  batch:
    job:
      enabled: false          # jobs are triggered on demand (REST/scheduler), NOT auto-run at startup
    cluster:
      enabled: true
      initialize-schema: never # the schema is created above via spring.sql.init

management:
  endpoints:
    web:
      exposure:
        include: health,info,batch-cluster,batch-cluster-jobs
```

`spring.batch.job.enabled=false` is important: without it, Spring Boot runs your job at startup — before
the cluster forms — and it fails. Trigger jobs yourself once the app is up.

That's it — your existing job and step definitions are unchanged. Next, implement a
`ClusterAwarePartitioner` and wire it with the `ClusterAwarePartitionHandler` — see the
**[Usage guide](guide.md)**.
