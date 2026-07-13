# Examples

The [`examples/`](https://github.com/jchejarla/spring-batch-db-cluster-partitioning/tree/main/examples)
module is a runnable Spring Boot application that stands up a real multi-node cluster and exposes jobs
over REST. Use it to see coordination, distribution, and failover end-to-end.

## Example jobs

| Job | Endpoint | What it shows |
|---|---|---|
| **Range-sum** | `GET /api/v1/clusteredjob/addition/taskSize/{n}/from/{from}/to/{to}` | Splits a numeric range into `n` partitions, sums each on a different worker, aggregates the total. |
| **ETL** | `GET /api/v1/etljob/rows/{rows}` | CSV → XML with chunk-oriented reader/processor/writer distributed across workers. |
| **Single-node baseline** | `GET /api/v1/singlenodejob/addition/...` | The same work without clustering, to compare timings. |

## Run it in 60 seconds (zero-setup H2)

The bundled `h2` profile uses a shared file-mode H2 database, so multiple JVMs on one machine form a
cluster with no external infrastructure:

```bash
# 1. Build
mvn clean package -pl examples -am

# 2. Start two nodes (separate terminals), sharing the H2 file
java -jar examples/target/examples-3.0.0-SNAPSHOT.jar --spring.profiles.active=h2 --server.port=8081
java -jar examples/target/examples-3.0.0-SNAPSHOT.jar --spring.profiles.active=h2 --server.port=8082

# 3. Trigger a clustered job on either node
curl http://localhost:8081/api/v1/clusteredjob/addition/taskSize/4/from/1/to/100
```

Each node prints a `>>>` lifecycle trace so you can watch partitions being claimed and completed across
both terminals, and the REST response returns the aggregated result.

## The worker tasklet

The work each partition runs is an ordinary Spring Batch `Tasklet` — it just reads its slice from the
`ExecutionContext`. Nothing in it is cluster-aware; distribution and failover happen underneath. This is
the actual example source:

```java
--8<-- "examples/src/main/java/examples/io/github/jchejarla/springbatch/clustering/simplejob/LargeSumExecutionTask.java:worker-task"
```

The `demo.partition.sleepMs` knob above is purely for demos — it stretches each partition so you can kill
a node mid-flight and watch failover (see below).

## Trying failover

Start three nodes, launch a job with partitions long enough to interrupt (`-Ddemo.partition.sleepMs`),
kill one worker, and watch its transferable partitions get reassigned to the survivors — the job still
completes with every partition run exactly once. The step-by-step walkthrough, along with the
PostgreSQL / MySQL / Oracle profiles and SQL for inspecting cluster state, is in the
[**full example README**](https://github.com/jchejarla/spring-batch-db-cluster-partitioning/blob/main/examples/README.md).

!!! note "Demo timings"
    The example profiles use deliberately fast failover timings so a dead node is removed in ~8s. Keep
    the [library defaults](configuration.md) for real deployments — see the note on that page.
