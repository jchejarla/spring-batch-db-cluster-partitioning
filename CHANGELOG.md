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