/*
 * Copyright 2025 Janardhan Chejarla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * Finds jobs whose current owner has left the cluster: coordination rows still in-flight
     * ({@code STARTED}, or {@code RECOVERING} if a previous reaper died) whose {@code master_node_id} no
     * longer exists in {@code batch_nodes} (the owner was marked unreachable and then removed by the
     * node-cleanup phase). Including {@code RECOVERING} ensures a recovery that was itself interrupted is
     * picked up by another node rather than being stranded.
     */
    default String getOrphanedMasterJobsQuery() {
        return "select bc.job_execution_id, bc.master_node_id, bc.master_step_execution_id, bc.master_step_name " +
                "from batch_job_coordination bc " +
                "where bc.status in ('STARTED', 'RECOVERING') " +
                "and not exists (select 1 from batch_nodes bn where bn.node_id = bc.master_node_id)";
    }

    /** Read-only: lists coordinated jobs (most recent first), for the job-centric observability view. */
    default String getCoordinatedJobsQuery() {
        return "select job_execution_id, master_node_id, master_step_execution_id, master_step_name, status " +
                "from batch_job_coordination order by created_time desc";
    }

    /** Read-only: the coordination row for one job execution. */
    default String getJobCoordinationByIdQuery() {
        return "select job_execution_id, master_node_id, master_step_execution_id, master_step_name, status " +
                "from batch_job_coordination where job_execution_id = ?";
    }

    /** Read-only: the partitions of a job (by manager step execution id) with their placement and status. */
    default String getPartitionsByMasterStepQuery() {
        return "select step_execution_id, partition_key, assigned_node, status " +
                "from batch_partitions where master_step_execution_id = ?";
    }

    /**
     * Atomically claims an orphaned coordination row for recovery: sets the transient status and takes
     * ownership ({@code master_node_id}), guarded by the (job execution, lost owner) pair. Because the
     * winner overwrites {@code master_node_id} with its own id, concurrent reapers see zero rows updated,
     * so a job is claimed exactly once; and if the winner later dies, the row's owner is gone again and it
     * is re-detected.
     */
    default String getClaimOrphanedMasterJobQuery() {
        return "update batch_job_coordination set status = ?, master_node_id = ?, last_updated = ? " +
                "where job_execution_id = ? and master_node_id = ? and status in ('STARTED', 'RECOVERING')";
    }

    default String getAllNodesInClusterQuery() {
        return "select node_id, created_time, last_updated_time, status, host_identifier, current_load from batch_nodes";
    }

    String getMarkNodesUnreachableQuery();
    String getDeleteNodesUnreachableQuery();

    /**
     * Returns a dialect-specific SQL expression for the elapsed milliseconds between {@code columnName}
     * and the current database time, used in heartbeat-age comparisons.
     * <p><strong>Security:</strong> {@code columnName} is interpolated directly into SQL, so it must always
     * be a trusted, hard-coded column name from this library — never user-supplied input. All <em>values</em>
     * in the generated queries are passed as bind parameters ({@code ?}), not concatenated.
     */
    String getTimeStampColumnWithDiffInMillisToCurrentTime(String columnName);
}
