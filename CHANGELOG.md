## [2.0.1] – 2026-06-13

### 🔒 Security

- Upgraded **Spring Boot** from `3.3.4` to `3.5.15`, which brings patched transitive dependencies — Tomcat `10.1.55`, Spring Framework `6.2.19`, and Jackson `2.21.4` — resolving all outstanding Dependabot security alerts (including several Critical and High severity CVEs in `tomcat-embed-core`).
- Bumped the PostgreSQL JDBC driver used by the examples from `42.7.5` to `42.7.11` (CVE-2025-49146, CVE-2026-42198).

### 🧹 Maintenance

- Removed hard-coded JUnit version pins and the custom Surefire/Failsafe overrides so the JUnit toolchain is managed coherently by the Spring Boot BOM.
- Pinned a literal project version in the parent POM (replacing the `${project.version}` expression) to ensure the reactor build resolves the in-tree parent.

> **Note:** This is a drop-in security patch. No public API changes. Spring Boot 3.x applications can upgrade by changing the dependency version to `2.0.1`.

## [2.0.0] – 2025-08-01

### 🚨 Breaking Changes

- **Artifact renamed** from `clustering-core` to `spring-batch-db-cluster-core`.  
  ⤷ Update your `pom.xml`:
  ```xml
  <dependency>
      <groupId>io.github.jchejarla</groupId>
      <artifactId>spring-batch-db-cluster-core</artifactId>
      <version>2.0.0</version>
  </dependency>
  ```
### ✨ New Features

- Added **Spring Boot Actuator integration** to improve observability of the distributed batch cluster.
- Introduced a new actuator endpoint:
  - `GET /actuator/batch-cluster`  
    → Returns status of all active nodes in the cluster, including heartbeat, last update time, and partition assignments.
  - `GET /actuator/batch-cluster/{nodeId}`  
    → Returns execution details for the specified node, such as assigned partitions, their status, and timestamps.
- Enhanced `/actuator/health` to reflect overall Spring Batch cluster health status, providing readiness and heartbeat validation.

## [1.0.1] – 2025-07-27

### Changed
- Refactored Java package naming to align with published Maven groupId `io.github.jchejarla`
  (thanks @arvind-tech-ai for the suggestion)

### Enhanced
- Added live task count tracking per node in `BATCH_NODES`
- Partition assignment logic now prioritizes nodes with fewer active tasks (improves load distribution and throughput)