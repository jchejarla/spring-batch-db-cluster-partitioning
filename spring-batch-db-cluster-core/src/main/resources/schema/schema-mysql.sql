-- NOTE: table names are lowercase to match the (lowercase) coordination queries. MySQL/MariaDB
-- table names are case-sensitive on case-sensitive filesystems (lower_case_table_names=0),
-- unlike the other dialects which fold identifier case. Keep our table names lowercase here.
-- maintains cluster nodes heartbeat
CREATE TABLE batch_nodes (
    NODE_ID VARCHAR(200) NOT NULL PRIMARY KEY,
    CREATED_TIME TIMESTAMP NOT NULL,
    LAST_UPDATED_TIME TIMESTAMP NOT NULL,
    STATUS VARCHAR(20) NOT NULL,
    HOST_IDENTIFIER VARCHAR(200),
    CURRENT_LOAD BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- Job coordination
CREATE TABLE batch_job_coordination (
    JOB_EXECUTION_ID BIGINT PRIMARY KEY,
    MASTER_NODE_ID VARCHAR(200) NOT NULL,
    MASTER_STEP_EXECUTION_ID BIGINT NOT NULL,
    MASTER_STEP_NAME VARCHAR(100) NOT NULL,
    STATUS VARCHAR(20) NOT NULL,
    CREATED_TIME TIMESTAMP NOT NULL,
    LAST_UPDATED TIMESTAMP NOT NULL,
    CONSTRAINT JOB_COORD_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ENGINE=InnoDB;

-- Partition tracking
CREATE TABLE batch_partitions (
    step_execution_id BIGINT PRIMARY KEY,
    job_execution_id BIGINT NOT NULL,
    partition_key VARCHAR(100) NOT NULL,
    assigned_node VARCHAR(100),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CLAIMED', 'COMPLETED', 'FAILED')),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    master_step_execution_id BIGINT NOT NULL,
    is_transferable SMALLINT DEFAULT 0,
    CHECK (is_transferable IN (0, 1)),
    CONSTRAINT BAT_PART_FK
        FOREIGN KEY (step_execution_id)
        REFERENCES BATCH_STEP_EXECUTION(step_execution_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Indexes for hot coordination queries (worker polling, completion checks, orphan scans)
CREATE INDEX IDX_BATCH_PART_NODE_STATUS ON batch_partitions (assigned_node, status);
CREATE INDEX IDX_BATCH_PART_MASTER_STATUS ON batch_partitions (master_step_execution_id, status);

-- Optional phase-timing event log (append-only; populated only when
-- spring.batch.cluster.capture-phase-timings=true). No FK: an audit trail that can outlive the job row.

CREATE TABLE batch_job_phase_events (
    JOB_EXECUTION_ID BIGINT NOT NULL,
    NODE_ID VARCHAR(200) NOT NULL,
    PHASE VARCHAR(40) NOT NULL,
    EVENT_TIME TIMESTAMP NOT NULL
) ENGINE=InnoDB;
CREATE INDEX IDX_BATCH_PHASE_EVENTS_JOB ON batch_job_phase_events (JOB_EXECUTION_ID);
