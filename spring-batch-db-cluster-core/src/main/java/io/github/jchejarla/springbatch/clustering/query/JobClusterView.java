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
package io.github.jchejarla.springbatch.clustering.query;

import java.util.List;
import java.util.Map;

/**
 * A read-only, job-centric view of how a job is distributed across the cluster: which node masters it,
 * how many partitions it has, the count of partitions in each status, and where each partition is running.
 *
 * @param jobExecutionId     the Spring Batch job execution id
 * @param masterNode         the node coordinating (mastering) this job
 * @param coordinationStatus the coordination status
 * @param partitionCount     total number of partitions
 * @param statusCounts       count of partitions by status (PENDING / CLAIMED / COMPLETED / FAILED)
 * @param partitions         per-partition placement and status
 */
public record JobClusterView(long jobExecutionId,
                             String masterNode,
                             String coordinationStatus,
                             int partitionCount,
                             Map<String, Long> statusCounts,
                             List<PartitionView> partitions) {
}
