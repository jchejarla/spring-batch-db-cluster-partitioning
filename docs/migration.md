# Migration Guide: 2.x → 3.0.0

Version 3.0.0 is a **platform release**. Alongside the new features, it moves to **Spring Boot 4.1,
Spring Batch 6, Spring Framework 7, and a Java 21 baseline**. The 2.x line stays on Spring Boot 3 for
maintenance (bug and security fixes).

This guide covers the changes you must make to upgrade. Most are one-liners; one — the JobRepository —
is new and required, so start there.

## Requirements

| | 2.x | 3.0.0 |
|---|---|---|
| Spring Boot | 3.x | 4.1+ |
| Spring Batch | 5.x | 6.x |
| Java | 17+ | **21+** |

## 1. Configure a JDBC JobRepository (required)

This is the most important change. **Spring Batch 6 defaults to an in-memory `ResourcelessJobRepository`**
— it keeps job metadata in the heap of a single JVM and persists nothing. A cluster cannot work on it:
worker nodes on other JVMs coordinate by reading job/partition metadata from the *shared database*, so
that metadata must be persisted with a JDBC-backed repository.

Opt into the JDBC repository on a configuration class:

```java
@SpringBootApplication
@EnableBatchProcessing
@EnableJdbcJobRepository   // dataSourceRef defaults to "dataSource", transactionManagerRef to "transactionManager"
public class MyApplication { }
```

Both annotations are needed: `@EnableJdbcJobRepository` is inert on its own — the registrar that reads
it comes from `@EnableBatchProcessing`.

!!! warning "Fail-fast guard"
    If you enable clustering (`spring.batch.cluster.enabled=true`) while the active repository is the
    resourceless default, the application **fails to start** with a clear message pointing you here —
    rather than launching jobs that silently never coordinate. (See *Troubleshooting* below for the
    symptom if you somehow bypass the guard.)

## 2. Initialize the Spring Batch schema yourself

Spring Boot 4 **removed Spring Batch's schema auto-initialization** — `spring.batch.jdbc.initialize-schema`
no longer exists. You now create the standard Spring Batch tables (`BATCH_JOB_EXECUTION`,
`BATCH_STEP_EXECUTION`, …) yourself, using one of:

- **A migration tool** (recommended for production) — Flyway or Liquibase.
- **`spring.sql.init`** — point it at the bundled Spring Batch DDL:

    ```yaml
    spring:
      sql:
        init:
          mode: always
          schema-locations: classpath:org/springframework/batch/core/schema-@@platform@@.sql
    ```

This library's own cluster tables are created **after** the Spring Batch schema (so the foreign keys
resolve) — the cluster auto-DDL (`spring.batch.cluster.initialize-schema`) is ordered with
`@DependsOnDatabaseInitialization`, so it runs after Flyway/Liquibase/`spring.sql.init`.

## 3. Update `ClusterAwareAggregator` construction

Spring Batch 6 removed the no-argument `RemoteStepExecutionAggregator` constructor (and the `JobExplorer`
API), so `ClusterAwareAggregator` now requires a `JobRepository`:

```java
// 2.x
ClusterAwareAggregator aggregator = new ClusterAwareAggregator(callback);
aggregator.setJobExplorer(jobExplorer);

// 3.0.0
ClusterAwareAggregator aggregator = new ClusterAwareAggregator(callback, jobRepository);
```

## 4. Other 3.0.0 breaking changes

These are independent of the platform bump:

- **`spring.batch.cluster.node-id` removed.** Node ids are generated as `<prefix>-<uuid>`. Use the
  optional `spring.batch.cluster.node-id-prefix` (defaults to the host name) instead.
- **`SCALE_UP` partitioning mode removed** (it duplicated round-robin). Use `ROUND_ROBIN` or the new
  `LEAST_LOADED`.
- **`PartitionAssignmentStrategy` now receives `List<ClusterNode>`** (carrying each node's live load)
  instead of `List<String>`. Custom strategies must update their signature and call `.nodeId()` on each
  node.

## Troubleshooting

**Symptom:** a clustered job fails with a foreign-key violation like
`JOB_COORD_FK … REFERENCES BATCH_JOB_EXECUTION` — i.e. the coordination row references a job execution
that isn't in the database.

**Cause:** you're on the in-memory `ResourcelessJobRepository`, so nothing was persisted to
`BATCH_JOB_EXECUTION`.

**Fix:** add `@EnableBatchProcessing` + `@EnableJdbcJobRepository` (step 1). The fail-fast guard should
catch this at startup; if you see the FK error, a `JobRepository` bean is being defined somewhere that
bypasses the guard.
