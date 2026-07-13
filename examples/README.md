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

- Java 21+
- Maven 3.x
- One of:
  - Nothing — the bundled H2 profile (Path A below) requires no external database, **or**
  - A PostgreSQL, MySQL, or Oracle server (for Path B below).

---

## Quick Start

You can run the example two ways. Pick one:

- **Path A — H2** (zero setup, recommended for a quick demo). The bundled `h2` profile uses an embedded file-mode database that multiple nodes share automatically, so no external database is required.
- **Path B — PostgreSQL** (closer to production). Provision a database first; run with the default `postgres` profile.

Both paths produce the same multi-node cluster behavior. Use Path A if you just want to verify functionality quickly; use Path B for performance benchmarking or production-like behavior.

---

### Path A — H2 (zero setup)

The `h2` profile uses an embedded H2 database in file-mode with `AUTO_SERVER=TRUE`. Multiple JVMs on the same machine share the same database file automatically, so multi-node clustering works without any additional infrastructure. Both the Spring Batch core schema and this library's cluster schema are created on first start. The database files are written to `./build/h2-cluster-demo*`; remove that directory to start fresh between runs.

#### A.1 Build the project

```bash
mvn clean package -pl examples -am
```

Each node generates its own unique `node-id` automatically (from the configured `${HOSTNAME}-${random.uuid}` default), so you do not need to set one. The `>>>` lifecycle output will show the generated id for each terminal.

#### A.2 Start Worker Node 1

```bash
java -jar examples/target/examples-3.0.0-SNAPSHOT.jar \
  --spring.profiles.active=h2 \
  --server.port=8081
```

#### A.3 Start Worker Node 2 (in a separate terminal)

```bash
java -jar examples/target/examples-3.0.0-SNAPSHOT.jar \
  --spring.profiles.active=h2 \
  --server.port=8082
```

The H2 web console is available at `http://localhost:8081/h2-console` (JDBC URL: `jdbc:h2:file:./build/h2-cluster-demo`, user `sa`, no password).

#### A.4 Trigger a job

Continue with [Trigger a Job](#trigger-a-job) below.

---

### Path B — PostgreSQL

#### B.1 Provision PostgreSQL

With Docker:

```bash
docker run --name sb-db \
  -e POSTGRES_USER=dev_user \
  -e POSTGRES_PASSWORD=dev_password \
  -e POSTGRES_DB=dev_db \
  -p 5432:5432 -d postgres:16

# Wait for postgres to accept connections
until docker exec sb-db pg_isready -U dev_user >/dev/null 2>&1; do sleep 1; done
```

Or, if you already have a local PostgreSQL server:

```bash
psql postgres <<'SQL'
CREATE USER dev_user WITH PASSWORD 'dev_password';
CREATE DATABASE dev_db OWNER dev_user;
GRANT ALL PRIVILEGES ON DATABASE dev_db TO dev_user;
SQL
```

#### B.2 Apply both schemas

The bundled `application-postgres.yml` uses `search_path=spring_batch,public`, so all tables go into a dedicated `spring_batch` schema:

> The `postgres` profile now bootstraps both schemas on startup via `spring.sql.init` (like the H2
> profile), so applying the table DDL below is **optional**. You still need to create the `spring_batch`
> namespace (the first command) — the profile's `search_path` points at it.

```bash
# Create the spring_batch namespace
PGPASSWORD=dev_password psql -h localhost -U dev_user -d dev_db \
  -c "CREATE SCHEMA IF NOT EXISTS spring_batch AUTHORIZATION dev_user;"

# Apply Spring Batch's own core schema (6.0.x)
curl -sSL https://raw.githubusercontent.com/spring-projects/spring-batch/6.0.x/spring-batch-core/src/main/resources/org/springframework/batch/core/schema-postgresql.sql \
  | PGPASSWORD=dev_password PGOPTIONS='--search_path=spring_batch,public' \
    psql -h localhost -U dev_user -d dev_db -v ON_ERROR_STOP=1

# Apply this library's cluster schema (from the repo)
PGPASSWORD=dev_password PGOPTIONS='--search_path=spring_batch,public' \
  psql -h localhost -U dev_user -d dev_db -v ON_ERROR_STOP=1 \
  -f spring-batch-db-cluster-core/src/main/resources/schema/schema-postgres.sql
```

You should now see all 10 tables (6 from Spring Batch core, 4 from this library) inside the `spring_batch` schema:

```bash
PGPASSWORD=dev_password psql -h localhost -U dev_user -d dev_db \
  -c "SELECT table_name FROM information_schema.tables WHERE table_schema='spring_batch' ORDER BY table_name;"
```

#### B.3 Build the project

From the repository root:

```bash
mvn clean package -pl examples -am
```

Each node generates its own unique `node-id` automatically (from the configured `${HOSTNAME}-${random.uuid}` default), so you do not need to set one. The `>>>` lifecycle output will show the generated id for each terminal.

#### B.4 Start Worker Node 1

```bash
java -jar examples/target/examples-3.0.0-SNAPSHOT.jar \
  --server.port=8081
```

#### B.5 Start Worker Node 2 (in a separate terminal)

```bash
java -jar examples/target/examples-3.0.0-SNAPSHOT.jar \
  --server.port=8082
```

You can start additional workers on different ports the same way.

#### B.6 Trigger a job

Continue with [Trigger a Job](#trigger-a-job) below.

---

## Trigger a Job

Send a request to any node. The node that receives the request becomes the master for that job execution:

```bash
# Smallest demo: split 1..100 into 4 partitions (corresponds to the
# sample console output in the next section)
curl http://localhost:8081/api/v1/clusteredjob/addition/taskSize/4/from/1/to/100

# Larger range, more partitions, useful for timing comparisons
curl http://localhost:8081/api/v1/clusteredjob/addition/taskSize/10/from/1/to/1000000

# ETL: process 1000 rows from the bundled CSV
curl http://localhost:8081/api/v1/etljob/rows/1000
```

The REST response shows the job ID, execution time, and computed result.

---

## What You'll See in the Console

The example prints a lifecycle trace prefixed with `>>>` on each node so you can watch the partition flow in real time. Each node-id is generated automatically (the auto-generated value combines the host name with a random UUID; the exact value will differ on your machine). After triggering the small job above (`taskSize/4/from/1/to/100`), the terminal running the **first node** (which received the request and acts as master) shows something like:

```
>>> [host-a1b2c3d4-...] (master) received job request: clustered-job taskSize=4 range=1..100
>>> [host-a1b2c3d4-...] (master) partitioner created 4 partitions for range 1..100 across 2 active worker node(s)
>>> [host-a1b2c3d4-...] (worker) picked up partition multiNodeWorkerStep:0 (range 1..25)
>>> [host-a1b2c3d4-...] (worker) picked up partition multiNodeWorkerStep:2 (range 51..75)
>>> [host-a1b2c3d4-...] (worker) completed partition multiNodeWorkerStep:0 result=325
>>> [host-a1b2c3d4-...] (worker) completed partition multiNodeWorkerStep:2 result=1575
>>> [host-a1b2c3d4-...] (master) aggregated 4 partition result(s); total sum=5050
>>> [host-a1b2c3d4-...] (master) job 1 finished in 1060 ms; total sum=5050
```

The terminal running the **second node** shows the two partitions it claimed and computed:

```
>>> [host-f5e6d7c8-...] (worker) picked up partition multiNodeWorkerStep:1 (range 26..50)
>>> [host-f5e6d7c8-...] (worker) picked up partition multiNodeWorkerStep:3 (range 76..100)
>>> [host-f5e6d7c8-...] (worker) completed partition multiNodeWorkerStep:1 result=950
>>> [host-f5e6d7c8-...] (worker) completed partition multiNodeWorkerStep:3 result=2200
```

The four partition results (`325 + 950 + 1575 + 2200 = 5050`) match the closed-form sum of integers from 1 to 100. Note that the master/worker distinction is per-job: the node that received the REST request acts as master for that job, while every active node — including the master — also executes worker tasks.

---

## Inspecting Cluster State

Independent of the console output, the database tables provide the full audit trail:

```sql
-- Active nodes and their last heartbeat
SELECT * FROM BATCH_NODES;

-- Partition assignments and current status (PENDING, CLAIMED, COMPLETED, FAILED)
SELECT * FROM BATCH_PARTITIONS;

-- Job-level coordination metadata
SELECT * FROM BATCH_JOB_COORDINATION;
```

For Path A (H2), use the web console linked in step A.3 — `http://localhost:8081/h2-console`. For Path B (PostgreSQL), these tables live in the `spring_batch` schema (`SELECT * FROM spring_batch.batch_nodes;`).

---

## Testing Failover

1. Start **three workers** (ports 8081, 8082, 8083).
2. Trigger a job with many partitions (e.g., `taskSize/20`). To keep partitions running long enough to
   kill a node mid-flight, add `-Ddemo.partition.sleepMs=15000` when starting the nodes so each
   partition takes ~15s.
3. While the job is running, **kill one worker** (e.g., `Ctrl+C` on worker-3).
4. With the example profiles' fast demo timings, the dead node is removed in ~8 seconds (3s to be
   marked `UNREACHABLE` + 5s cleanup), after which:
   - The killed node is marked `UNREACHABLE` and then removed from `BATCH_NODES`.
   - Its incomplete **transferable** partitions (`CLAIMED` or `PENDING`) are reassigned to the surviving
     workers.
   - The job completes successfully — no partition is lost or executed twice.

> **Note on timing:** the example profiles use aggressive demo timings so failover is quick to watch. A
> real deployment should keep the library defaults (a dead node is removed in ~75s), which tolerate GC
> pauses and brief network blips without prematurely evicting a healthy node. See the
> [Configuration reference](../docs/configuration.md).

---

## Monitoring

The application exposes Spring Boot Actuator endpoints for cluster observability:

```bash
# Cluster health (included in overall health check)
curl http://localhost:8081/actuator/health

# Cluster overview — all nodes and their current executions
curl http://localhost:8081/actuator/batch-cluster

# Details for a specific node (replace {node-id} with one of the ids
# from the cluster overview above)
curl http://localhost:8081/actuator/batch-cluster/{node-id}
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
    application-postgres.yml           PostgreSQL connection config (default)
    application-mysql.yml              MySQL connection config
    application-oracle.yml             Oracle connection config
    application-h2.yml                 H2 file-mode profile (zero-setup)
  pom.xml                              Maven build for the examples module
```

---

## Configuration Reference

Key properties (set in `application-*.yml` or as command-line overrides):

| Property | Default | Description |
|----------|---------|-------------|
| `spring.batch.cluster.enabled` | `false` | Enable database-driven cluster partitioning |
| `spring.batch.cluster.node-id-prefix` | host name | Readable prefix for this node's id; a unique suffix is appended per JVM and per restart |
| `spring.batch.cluster.heartbeat-interval` | `3000` | Milliseconds between heartbeat writes |
| `spring.batch.cluster.task-polling-interval` | `1000` | Milliseconds between partition-polling cycles |
| `spring.batch.cluster.unreachable-node-threshold` | `10000` | Milliseconds before a silent node is marked unreachable |
| `spring.batch.cluster.node-cleanup-threshold` | `30000` | Milliseconds before an unreachable node is removed and partitions reassigned |

> The values above are the library **defaults**. The bundled example profiles deliberately shorten them
> (`heartbeat-interval: 1000`, `unreachable-node-threshold: 3000`, `node-cleanup-threshold: 5000`) so
> failover is quick to demo — keep the defaults for real deployments. The
> [full configuration reference](../docs/configuration.md)
> lists every property.

---

## Using MySQL or Oracle

Path B above uses PostgreSQL because it most closely resembles production deployments. Pre-configured profiles also exist for **MySQL** and **Oracle**. To use one:

1. **Provision the database** with credentials that match `application-mysql.yml` or `application-oracle.yml` (defaults: database `dev_db`, user `dev_user`, password `dev_password`).
2. **Apply both schemas** the same way as in step B.2, substituting the appropriate DDL files:
    - Spring Batch core DDL: download from [`spring-projects/spring-batch:6.0.x/.../schema-mysql.sql`](https://github.com/spring-projects/spring-batch/blob/6.0.x/spring-batch-core/src/main/resources/org/springframework/batch/core/schema-mysql.sql) or [`schema-oracle.sql`](https://github.com/spring-projects/spring-batch/blob/6.0.x/spring-batch-core/src/main/resources/org/springframework/batch/core/schema-oracle.sql).
    - Cluster DDL: `spring-batch-db-cluster-core/src/main/resources/schema/schema-mysql.sql` or `schema-oracle.sql` from this repository.
3. **Activate the profile** when starting each node:
   ```bash
   java -jar examples/target/examples-3.0.0-SNAPSHOT.jar \
     --spring.profiles.active=mysql \
     --server.port=8081
   ```
