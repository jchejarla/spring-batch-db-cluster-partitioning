# Configuration

All settings live under the `spring.batch.cluster.*` namespace and are bound by `BatchClusterProperties`.
Clustering is opt-in (`enabled=true`). Intervals and thresholds are in **milliseconds**; the defaults
suit small-to-medium clusters (~2–20 nodes).

--8<-- "docs/_snippets/config-properties.md"

<small>(The table above is generated from the `@ConfigurationProperties` source, so it never drifts from the code. The deprecated `node-id` property is intentionally omitted — see the note at the end of this page.)</small>

!!! note "The bundled example profiles use fast demo timings — don't copy them to production"
    So you can watch failover in seconds, the example profiles (`application-h2.yml`,
    `application-postgres.yml`, `application-mysql.yml`, `application-oracle.yml`) deliberately shorten
    these to `heartbeat-interval: 1000`, `unreachable-node-threshold: 3000`, `node-cleanup-threshold:
    5000` — so a dead node is removed in ~8s. **Real deployments should keep the defaults above**
    (a dead node is removed in ~75s): the longer thresholds tolerate GC pauses and brief network blips
    without prematurely evicting a healthy node and reassigning work it is still running.

!!! note "Spring Batch core schema is a prerequisite"
    These tables have foreign keys to the standard Spring Batch tables, so the Spring Batch schema must
    exist first. Spring Boot 4 no longer auto-creates it — use Flyway/Liquibase or `spring.sql.init`
    (see [Installation](installation.md)). The cluster auto-DDL runs after it via
    `@DependsOnDatabaseInitialization`.

!!! warning "Actuator exposure"
    The `/actuator/batch-cluster` endpoints expose node and partition detail. Restrict their exposure
    and protect them with authentication; do not expose them unauthenticated on a public interface.

The legacy `node-id` property has been removed (ids are generated automatically); a leftover value is
ignored with a warning. Use `node-id-prefix` instead.
