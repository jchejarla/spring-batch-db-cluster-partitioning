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

/**
 * Read-only view of a single partition's placement and status within a job.
 *
 * @param stepExecutionId the partition's Spring Batch step execution id
 * @param partitionKey    the partition key
 * @param assignedNode    the node currently assigned to execute it
 * @param status          the partition status (PENDING / CLAIMED / COMPLETED / FAILED)
 */
public record PartitionView(long stepExecutionId, String partitionKey, String assignedNode, String status) {
}
