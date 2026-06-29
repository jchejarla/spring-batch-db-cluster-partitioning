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
