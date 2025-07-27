package io.github.jchejarla.springbatch.clustering.core;

import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties.HostIdentifier;
import io.github.jchejarla.springbatch.clustering.mgmt.ClusterNode;
import io.github.jchejarla.springbatch.clustering.mgmt.NodeLoad;
import io.github.jchejarla.springbatch.clustering.mgmt.NodeStatus;
import io.github.jchejarla.springbatch.clustering.polling.PartitionAssignmentTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DatabaseBackedClusterService {

    private final JdbcTemplate jdbcTemplate;
    private final BatchClusterProperties batchClusterProperties;
    private final DBSpecificQueryProvider queryProvider;

    @Transactional
    public int registerNode() {
        String hostIdentifier = null;
        try {
            InetAddress host = InetAddress.getLocalHost();
            if (HostIdentifier.IP_ADDRESS == batchClusterProperties.getHostIdentifier()) {
                hostIdentifier = host.getHostAddress();
            } else {
                hostIdentifier = host.getHostName();
            }
        } catch (Exception e) {
            log.error("Error occurred while getting host identifier", e);
        }
        Object[] params = new Object[]{batchClusterProperties.getNodeId(), new Date(), new Date(), NodeStatus.ACTIVE.name(), hostIdentifier};
        return jdbcTemplate.update(queryProvider.getInsertQueryToRegisterNodeQuery(), params);
    }

    @Transactional
    public int updateNodeHeartbeat() {
        Object[] params = new Object[]{new Date(), NodeStatus.ACTIVE.name(), NodeLoad.INST.getCurrentLoad(), batchClusterProperties.getNodeId()};
        return jdbcTemplate.update(queryProvider.getUpdateNodeHeartBeatQuery(), params);
    }

    @Transactional
    public int markNodesUnreachable() {
        Object[] params = new Object[]{NodeStatus.UNREACHABLE.name(), NodeStatus.ACTIVE.name(), batchClusterProperties.getUnreachableNodeThreshold()};
        return jdbcTemplate.update(queryProvider.getMarkNodesUnreachableQuery(), params);
    }

    @Transactional
    public int deleteNodesUnreachable() {
        Object[] params = new Object[]{NodeStatus.UNREACHABLE.name(), batchClusterProperties.getNodeCleanupThreshold()};
        return jdbcTemplate.update(queryProvider.getDeleteNodesUnreachableQuery(), params);
    }

    @Transactional
    public int saveBatchJobCoordinationInfo(long jobExecutionId, long masterStepExecutionId, String masterStepName) {
        Object[] params = new Object[]{jobExecutionId, batchClusterProperties.getNodeId(), masterStepExecutionId, masterStepName, "CREATED", new Date(), new Date()};
        return jdbcTemplate.update(queryProvider.getSaveBatchJobCoordinationInfoQuery(), params);
    }

    @Transactional
    public int updateBatchJobCoordinationStatus(long jobExecutionId, long masterStepExecutionId, String newStatus) {
        Object[] params = new Object[]{newStatus, new Date(), jobExecutionId, masterStepExecutionId};
        return jdbcTemplate.update(queryProvider.getUpdateBatchJobCoordinationStatusQuery(), params);
    }

    @Transactional
    public int[] saveBatchPartitions(List<Object[]> params) {
        return jdbcTemplate.batchUpdate(queryProvider.getSaveBatchPartitionsQuery(), params);
    }

    @Transactional
    public int[] updateBatchPartitionsToReAssignedNodes(List<Object[]> params) {
        return jdbcTemplate.batchUpdate(queryProvider.getUpdateBatchPartitionsToReAssignedNodesQuery(), params);
    }

    public Integer getPendingTasksCount(long masterStepExecutionId) {
        return jdbcTemplate.queryForObject(queryProvider.getPendingTasksCountQuery(), Integer.class, masterStepExecutionId);
    }

    /**
     * @return - assigned tasks for the current node, taking consideration of currently running job_execution_id and step_execution_id and if the master node is still healthy for the job.
     */
    public List<PartitionAssignmentTask> fetchPartitionAssignedTasks() {
        return jdbcTemplate.query(queryProvider.getFetchPartitionAssignedTasksQuery(),
                (rs, rowNum) -> new PartitionAssignmentTask(
                        rs.getLong("job_execution_id"),
                        rs.getString("partition_key"),
                        rs.getLong("step_execution_id"),
                        rs.getLong("master_step_execution_id"),
                        rs.getInt("is_transferable") != 0,
                        rs.getString("master_step_name"),
                        batchClusterProperties.getNodeId()
                ),
                batchClusterProperties.getNodeId()
        );
    }

    @Transactional
    public void updatePartitionsStatus(Collection<PartitionAssignmentTask> partitionAssignmentTasks, String status) {
        List<Object[]> rows = partitionAssignmentTasks.stream().map(partitionAssignmentTask -> new Object[]{status, partitionAssignmentTask.stepExecutionId(), partitionAssignmentTask.jobExecutionId(), partitionAssignmentTask.masterStepExecutionId(), partitionAssignmentTask.assignedNode()}).toList();
        jdbcTemplate.batchUpdate(queryProvider.getUpdatePartitionStatusToQuery(), rows);
    }

    @Transactional
    public void updatePartitionsLastUpdatedTime(Collection<PartitionAssignmentTask> partitionAssignmentTasks) {
        List<Object[]> rows = partitionAssignmentTasks.stream().map(partitionAssignmentTask -> new Object[]{partitionAssignmentTask.stepExecutionId(), partitionAssignmentTask.jobExecutionId(), partitionAssignmentTask.masterStepExecutionId(), partitionAssignmentTask.assignedNode()}).toList();
        jdbcTemplate.batchUpdate(queryProvider.getUpdateLastUpdateTimeQuery(), rows);
    }

    @Transactional
    public void updatePartitionStatus(PartitionAssignmentTask partitionAssignmentTask, String status) {
        updatePartitionsStatus(List.of(partitionAssignmentTask), status);
    }


    public List<PartitionAssignmentTask> checkForOrphanedTasks(long masterStepExecutionId) {
        return jdbcTemplate.query(queryProvider.getCheckForOrphanedTasksQuery(),
                (rs, rowNum) -> new PartitionAssignmentTask(
                        rs.getLong("job_execution_id"),
                        rs.getString("partition_key"),
                        rs.getLong("step_execution_id"),
                        rs.getLong("master_step_execution_id"),
                        rs.getBoolean("is_transferable"),
                        rs.getString("master_step_name"),
                        rs.getString("assigned_node")
                ), masterStepExecutionId, batchClusterProperties.getNodeCleanupThreshold()
        );
    }

    public List<ClusterNode> getActiveNodes() {
        return jdbcTemplate.query(queryProvider.getActiveNodesQuery(),
                (rs, rowNum) -> new ClusterNode(
                        rs.getString("node_id"),
                        rs.getLong("current_load")
                )
        );
    }

}
