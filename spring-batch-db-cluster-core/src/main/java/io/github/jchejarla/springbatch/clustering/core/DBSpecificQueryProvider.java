package io.github.jchejarla.springbatch.clustering.core;

public interface DBSpecificQueryProvider {

    default String getInsertQueryToRegisterNodeQuery() {
        return "insert into batch_nodes (node_id, created_time, last_updated_time, status, host_identifier) values (?, ?, ?, ?, ?)";
    }

    default String getUpdateNodeHeartBeatQuery() {
        return "update batch_nodes set last_updated_time = ?, status = ?, current_load=? where node_id =?";
    }

    default String getSaveBatchJobCoordinationInfoQuery() {
        return "insert into batch_job_coordination (job_execution_id, master_node_id, master_step_execution_id, master_step_name, status, created_time, last_updated) values (?, ?, ?, ?, ?, ?, ?)";
    }

    default String getUpdateBatchJobCoordinationStatusQuery() {
        return "update batch_job_coordination set status = ?, last_updated = ? where job_execution_id = ? and master_step_execution_id = ? ";
    }

    default String getSaveBatchPartitionsQuery() {
        return "insert into batch_partitions (step_execution_id, job_execution_id, partition_key, assigned_node, status, master_step_execution_id, is_transferable) values (?, ?, ?, ?, ?, ?, ?)";
    }

    default String getUpdateBatchPartitionsToReAssignedNodesQuery() {
        return "update batch_partitions set assigned_node=?, last_updated = ?, status = 'PENDING' where job_execution_id = ? and master_step_execution_id=? and step_execution_id = ? ";
    }

    default String getPendingTasksCountQuery(){
        return "select count(*) from batch_partitions where master_step_execution_id = ? and status in ('PENDING', 'CLAIMED')";
    }

    default String getFetchPartitionAssignedTasksQuery() {
        return "select bp.status, bp.job_execution_id, bp.partition_key, bp.step_execution_id, bp.master_step_execution_id, bp.is_transferable, bc.master_step_name " +
                "from batch_partitions bp, batch_job_coordination bc, batch_nodes bn " +
                "where bp.master_step_execution_id = bc.master_step_execution_id " +
                "and bc.master_node_id = bn.node_id " +
                "and bp.assigned_node = ? " +
                "and bp.status = 'PENDING' " +
                "and bc.status='STARTED'";
    }

    default String getUpdatePartitionStatusToQuery() {
        return "update batch_partitions set status = ?, last_updated = CURRENT_TIMESTAMP where step_execution_id = ? and job_execution_id = ? and master_step_execution_id = ? and assigned_node = ?";
    }

    default String getUpdateLastUpdateTimeQuery() {
        return "update batch_partitions set last_updated = CURRENT_TIMESTAMP where step_execution_id = ? and job_execution_id = ? and master_step_execution_id = ? and assigned_node = ?";
    }

    default String getCheckForOrphanedTasksQuery() {
        return "select bp.job_execution_id, bp.partition_key, bp.step_execution_id, bp.master_step_execution_id, bp.is_transferable, bp.assigned_node, bc.master_step_name " +
                "from batch_partitions bp, batch_job_coordination bc " +
                "where bp.master_step_execution_id = bc.master_step_execution_id " +
                "and bp.master_step_execution_id = ? " +
                "and bp.status in ('PENDING','CLAIMED') " +
                "and "+getTimeStampColumnWithDiffInMillisToCurrentTime("bp.last_updated")+ " >= ? "+
                "and not exists (select 1 from batch_nodes bn where bn.node_id = bp.assigned_node)";
    }

    default String getActiveNodesQuery() {
        return "select node_id, current_load from batch_nodes where status='ACTIVE' order by current_load asc";
    }

    /**
     * Finds jobs whose master node has left the cluster: coordination rows still marked {@code STARTED}
     * whose {@code master_node_id} no longer exists in {@code batch_nodes} (i.e. the master was marked
     * unreachable and then removed by the node-cleanup phase). These are candidates for recovery.
     */
    default String getOrphanedMasterJobsQuery() {
        return "select bc.job_execution_id, bc.master_node_id, bc.master_step_execution_id, bc.master_step_name " +
                "from batch_job_coordination bc " +
                "where bc.status = 'STARTED' " +
                "and not exists (select 1 from batch_nodes bn where bn.node_id = bc.master_node_id)";
    }

    /**
     * Atomically claims an orphaned coordination row for recovery, transitioning it from {@code STARTED}
     * to a caller-supplied transient status, guarded by the (job execution, dead master) pair. Exactly one
     * surviving node wins the claim (the database transaction arbitrates), so a job is reaped only once.
     */
    default String getClaimOrphanedMasterJobQuery() {
        return "update batch_job_coordination set status = ?, last_updated = ? " +
                "where job_execution_id = ? and status = 'STARTED' and master_node_id = ?";
    }

    default String getAllNodesInClusterQuery() {
        return "select node_id, created_time, last_updated_time, status, host_identifier, current_load from batch_nodes";
    }

    String getMarkNodesUnreachableQuery();
    String getDeleteNodesUnreachableQuery();
    String getTimeStampColumnWithDiffInMillisToCurrentTime(String columnName);
}
