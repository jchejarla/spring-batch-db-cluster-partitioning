## [1.0.1] – 2025-07-27

### Changed
- Refactored Java package naming to align with published Maven groupId `io.github.jchejarla`
  (thanks @arvind-tech-ai for the suggestion)

### Enhanced
- Added live task count tracking per node in `BATCH_NODES`
- Partition assignment logic now prioritizes nodes with fewer active tasks (improves load distribution and throughput)