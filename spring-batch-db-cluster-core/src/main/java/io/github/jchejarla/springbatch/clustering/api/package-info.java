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
/**
 * User-facing extension points for building cluster-aware partitioned jobs.
 *
 * <p>Subclass {@code ClusterAwarePartitioner} to split work and choose a {@code PartitionStrategy},
 * and implement {@code ClusterAwareAggregator} / {@code ClusterAwareAggregatorCallback} to aggregate
 * partition results and react to overall job success or failure. These are the only types most
 * applications need to interact with; the rest of the library is wired automatically.</p>
 */
package io.github.jchejarla.springbatch.clustering.api;
