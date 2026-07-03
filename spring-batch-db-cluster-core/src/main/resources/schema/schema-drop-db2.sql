-- Db2 has no DROP TABLE IF EXISTS; run with continue-on-error,
-- the same way Spring Batch ships its own Db2 drop script.
DROP TABLE BATCH_PARTITIONS;
DROP TABLE BATCH_JOB_COORDINATION;
DROP TABLE BATCH_NODES;
DROP TABLE BATCH_JOB_PHASE_EVENTS;
