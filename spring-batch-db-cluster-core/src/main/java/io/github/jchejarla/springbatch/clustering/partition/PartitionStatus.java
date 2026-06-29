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
package io.github.jchejarla.springbatch.clustering.partition;

/**
 * Lifecycle states of a single partition row in {@code BATCH_PARTITIONS}.
 *
 * <p>The {@code name()} of each value is the exact string persisted in the {@code STATUS} column, and
 * must stay in sync with the {@code CHECK} constraint in the bundled schema DDL. This enum is the
 * single source of truth for partition status values across the framework.</p>
 *
 * @author Janardhan Chejarla
 */
public enum PartitionStatus {
    PENDING,
    CLAIMED,
    COMPLETED,
    FAILED
}
