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
 * Built-in partition-assignment strategies: round-robin, fixed-node-count, and least-loaded.
 *
 * <p>Each maps the work units produced by the partitioner onto the available nodes; the strategy is
 * selected at runtime via the partitioner's chosen {@code PartitionStrategy}. The least-loaded
 * strategy is load-aware, using the live per-node load reported in {@code BATCH_NODES}.</p>
 */
package io.github.jchejarla.springbatch.clustering.partition.impl;
