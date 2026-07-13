# Security

This page is about **hardening a deployment**. To report a vulnerability, follow the private
disclosure process in
[SECURITY.md](https://github.com/jchejarla/spring-batch-db-cluster-partitioning/blob/main/SECURITY.md)
— email, not a public issue.

## Trust model: the database is the security boundary

Coordination happens entirely through the database, so the coordination tables (`BATCH_NODES`,
`BATCH_PARTITIONS`, `BATCH_JOB_COORDINATION`) **are** the trust anchor. There is no separate
node-to-node authentication — nodes trust each other by virtue of shared database access. Any principal
that can write to those tables can register a node, claim or reassign partitions, or influence job
coordination.

The practical consequence: **securing the coordination database is securing the cluster.** The three
controls below follow from that.

## 1. Least-privilege database account

At runtime the application only needs **DML** (`SELECT`/`INSERT`/`UPDATE`/`DELETE`) on the Spring Batch
tables and this library's cluster tables — not schema-wide DDL or admin rights.

- Grant DML on the specific tables; do **not** grant `CREATE`/`DROP`/owner rights to the runtime account.
- Create the schema ahead of time with a migration tool (Flyway/Liquibase) and set
  `spring.batch.cluster.initialize-schema=never` (see [Configuration](configuration.md)), so the runtime
  account never needs `CREATE TABLE`. Auto-DDL is a development convenience, not a production one.
- Use distinct credentials per environment. Never commit them — inject via environment variables or a
  secrets manager.

## 2. Network isolation

Keep the coordination database reachable **only from the cluster nodes** — a private subnet, security
group, or firewall rule. Its exposure surface is exactly that of any application database; treat it the
same way, including TLS on the connection.

## 3. Actuator endpoints

The cluster endpoints (`batch-cluster`, `batch-cluster-jobs`, `info`, `health`) are **read-only** — none
of them mutate cluster state — but they **disclose operational detail**: host names, node ids, and where
each partition is running. They are **not exposed over HTTP by default**. If you expose them:

- Require authentication/authorization (e.g. Spring Security).
- Prefer a **separate, firewalled management port** (`management.server.port`) reachable only from your
  ops network.
- Never expose `batch-cluster` / `batch-cluster-jobs` unauthenticated on a public interface.

See [Observability](Observability.md) for the endpoint details.

## What the library does — and does not — do

- **Does not** add authentication or transport security. That is the application's responsibility
  (Spring Security for HTTP, TLS to the database). The library imposes no auth so it composes with
  whatever you already run.
- **Parameterizes all SQL** — queries are static strings with bound parameters, so values (including
  node ids and the actuator's `jobExecutionId`) cannot be used for SQL injection.
- **Performs no deserialization of untrusted data.** Partition payloads travel as Spring Batch
  `ExecutionContext` metadata through the shared repository, not as ad-hoc serialized objects.
- **All actuator operations are read-only** — there is no write/delete endpoint to abuse.

## Supply chain

Releases ship a CycloneDX SBOM, and Dependabot watches the Maven, GitHub Actions, and docs-tooling
dependencies for known-vulnerable versions. Keep to the latest release to receive security fixes (see
the [support policy](https://github.com/jchejarla/spring-batch-db-cluster-partitioning/blob/main/SECURITY.md)).
