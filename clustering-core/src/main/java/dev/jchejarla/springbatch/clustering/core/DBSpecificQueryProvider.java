package dev.jchejarla.springbatch.clustering.core;

public interface DBSpecificQueryProvider {

    default String getInsertQueryToRegisterNodeQuery() {
        return "insert into batch_nodes (node_id, created_time, last_updated_time, status, host_identifier) values (?, ?, ?, ?, ?)";
    }

    default String getUpdateNodeHeartBeatQuery() {
        return "update batch_nodes set last_updated_time = ?, current_load=? where node_id =?";
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
        return "update batch_partitions set status = ?, last_updated = CURRENT_TIMESTAMP where step_execution_id = ? and job_execution_id = ? and master_step_execution_id = ?";
    }

    default String getCheckForOrphanedTasksQuery() {
        return "select bp.job_execution_id, bp.partition_key, bp.step_execution_id, bp.master_step_execution_id, bp.is_transferable, bp.assigned_node, bc.master_step_name " +
                "from batch_partitions bp, batch_job_coordination bc " +
                "where bp.master_step_execution_id = bc.master_step_execution_id " +
                "and bp.master_step_execution_id = ? " +
                "and bp.status in ('PENDING','CLAIMED') " +
                "and not exists (select 1 from batch_nodes bn where bn.node_id = bp.assigned_node)";
    }

    default String getActiveNodesQuery() {
        return "select node_id, current_load from batch_nodes where status='ACTIVE' order by current_load desc";
    }

    String getMarkNodesUnreachableQuery();
    String getDeleteNodesUnreachableQuery();
}
