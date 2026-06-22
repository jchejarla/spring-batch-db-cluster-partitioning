package io.github.jchejarla.springbatch.clustering.mgmt;

/**
 * A job whose master node has left the cluster while the job was still running.
 *
 * <p>Produced by the recovery scan when a {@code BATCH_JOB_COORDINATION} row is still marked
 * {@code STARTED} but its master node is no longer present in {@code BATCH_NODES}. Such a job can
 * no longer make progress on its own (the node that was monitoring and aggregating it is gone), so
 * a surviving node reaps it.</p>
 *
 * @param jobExecutionId        the Spring Batch job execution id of the stranded job
 * @param masterNodeId          the id of the master node that was lost
 * @param masterStepExecutionId the manager step execution id that coordinated the partitions
 * @param masterStepName        the manager step name
 * @author Janardhan Chejarla
 */
public record OrphanedMasterJob(long jobExecutionId, String masterNodeId, long masterStepExecutionId, String masterStepName) {
}
