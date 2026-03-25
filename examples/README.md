# Examples: Spring Batch DB Cluster Partitioning

This folder contains a runnable example application that demonstrates the core functional claims of the
**spring-batch-db-cluster-partitioning** extension:

1. **Database-driven coordination** of partitioned Spring Batch steps across multiple workers/nodes.
2. **Cluster-safe partition execution** with deterministic partition assignment and completion tracking.
3. **Restartability and failure recovery** without reprocessing completed partitions.
4. **Horizontal scalability**: increase throughput by adding workers without changing job logic.

The example is intentionally minimal so that users can run it locally and validate behavior using only a database and a JVM.

---

## Example Jobs

The application exposes two example jobs via REST endpoints:

| Job | Endpoint | Description |
|-----|----------|-------------|
| **Range-sum (simple)** | `GET /api/v1/clusteredjob/addition/taskSize/{n}/from/{from}/to/{to}` | Splits a numeric range into `n` partitions, computes the sum in each partition on a different worker, and aggregates the result. |
| **ETL (advanced)** | `GET /api/v1/etljob/rows/{rows}` | Reads rows from a CSV file (`data/customers_example_data.csv`), distributes chunks across workers using round-robin, and writes XML output. Demonstrates chunk-oriented reader/processor/writer with cluster partitioning. |

There is also a **single-node baseline** endpoint (`GET /api/v1/singlenodejob/addition/...`) so you can compare execution time with and without clustering.

---

## Prerequisites

- Java 17+ (or the version required by this repository)
- Maven 3.x
- A relational database supported by Spring Batch (PostgreSQL, MySQL, Oracle, or H2 for local testing)

> **Tip:** Use PostgreSQL for the closest-to-production behavior. H2 is fine for quick local validation.

---

## Quick Start (single machine, multiple workers)

### 1. Start the database

If you use Docker:

```bash
docker run --name sb-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres -e POSTGRES_DB=sb \
  -p 5432:5432 -d postgres:16
```

Create the Spring Batch schema and the three cluster coordination tables. DDL scripts are provided in
`spring-batch-db-cluster-core/src/main/resources/` (choose the file matching your database).

### 2. Configure the database connection

Edit `src/main/resources/application-postgres.yml` (or the profile matching your database) to point to your running instance:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sb
    username: postgres
    password: postgres
```

The active profile is set in `application.properties`:

```properties
spring.profiles.active=postgres
```

Pre-configured profiles are available for **PostgreSQL**, **MySQL**, and **Oracle**.

### 3. Build the project

From the repository root:

```bash
mvn clean package -pl examples -am
```

### 4. Start Worker Node 1 (becomes master)

```bash
java -jar examples/target/examples-2.0.0.jar \
  --server.port=8081 \
  --spring.batch.cluster.node-id=worker-1
```

### 5. Start Worker Node 2

In a separate terminal:

```bash
java -jar examples/target/examples-2.0.0.jar \
  --server.port=8082 \
  --spring.batch.cluster.node-id=worker-2
```

You can start additional workers on different ports in the same way.

### 6. Trigger the job

Send a request to any node. The node that receives the request becomes the master for that job execution:

```bash
# Range-sum job: 10 partitions, sum numbers from 1 to 1,000,000
curl http://localhost:8081/api/v1/clusteredjob/addition/taskSize/10/from/1/to/1000000
```

```bash
# ETL job: process 1000 rows from the CSV
curl http://localhost:8081/api/v1/etljob/rows/1000
```

### 7. Observe the results

- The REST response shows the job ID, execution time, and computed result.
- Check the database tables for partition state:
  ```sql
  SELECT * FROM BATCH_NODES;           -- active nodes and heartbeats
  SELECT * FROM BATCH_PARTITIONS;      -- partition assignments and statuses
  SELECT * FROM BATCH_JOB_COORDINATION; -- job-level coordination metadata
  ```

---

## Testing Failover

1. Start **three workers** (ports 8081, 8082, 8083).
2. Trigger a job with many partitions (e.g., `taskSize/20`).
3. While the job is running, **kill one worker** (e.g., `Ctrl+C` on worker-3).
4. After the heartbeat timeout (default 15 seconds), observe that:
   - The killed node is marked `UNREACHABLE` in `BATCH_NODES`.
   - Its incomplete partitions (status `CLAIMED` or `PENDING`) are reassigned to the surviving workers.
   - The job completes successfully without any partitions being lost or reprocessed.

---

## Monitoring

The application exposes Spring Boot Actuator endpoints for cluster observability:

```bash
# Cluster health (included in overall health check)
curl http://localhost:8081/actuator/health

# Cluster overview — all nodes and their current executions
curl http://localhost:8081/actuator/batch-cluster

# Details for a specific node
curl http://localhost:8081/actuator/batch-cluster/worker-1
```

---

## Project Structure

```
examples/
  data/
    customers_example_data.csv         Sample CSV data for the ETL job
  src/main/java/.../clustering/
    StartApp.java                      Spring Boot application entry point
    RestEndPoints.java                 REST endpoints to trigger jobs
    simplejob/
      SimpleJobConfig.java             Range-sum job with ClusterAwarePartitioner
      LargeSumExecutionTask.java       Tasklet that computes the sum of a range
      SingleNodeExecutionTask.java     Single-node baseline for comparison
    advancedjob/
      ETLJobConfig.java                CSV-to-XML ETL job with round-robin partitioning
      Customer.java                    Domain model
      CustomerProcessor.java           Item processor
      CSVItemReaderConfig.java         Flat-file reader configuration
      WriterFactory.java               XML writer factory
    taskexec/
      TaskExecJobConfig.java           Alternative job config using TaskExecutor
      SumAggregator.java               Custom aggregation callback
    common/
      Range.java, RangeItemReader.java Helper classes for range-based partitioning
      RangeSumProcessor.java           Processor for range-sum computation
      RangeSumWriter.java              Writer for range-sum results
    messaging/
      ActiveMQConfig.java              (For comparison) message-channel partitioning
      MasterConfig.java                Message-channel master config
      WorkerConfig.java                Message-channel worker config
  src/main/resources/
    application.properties             Default profile selection
    application-postgres.yml           PostgreSQL connection config
    application-mysql.yml              MySQL connection config
    application-oracle.yml             Oracle connection config
  pom.xml                             Maven build for the examples module
```

---

## Configuration Reference

Key properties (set in `application-*.yml` or as command-line overrides):

| Property | Default | Description |
|----------|---------|-------------|
| `spring.batch.cluster.enabled` | `false` | Enable database-driven cluster partitioning |
| `spring.batch.cluster.node-id` | — | Unique identifier for this node instance |
| `spring.batch.cluster.heartbeat-interval` | `5000` | Milliseconds between heartbeat writes |
| `spring.batch.cluster.task-polling-interval` | `2000` | Milliseconds between partition-polling cycles |
| `spring.batch.cluster.unreachable-node-threshold` | `15000` | Milliseconds before a silent node is marked unreachable |
| `spring.batch.cluster.node-cleanup-threshold` | `60000` | Milliseconds before an unreachable node is removed and partitions reassigned |
