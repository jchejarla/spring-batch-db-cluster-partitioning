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

/**
 * Lifecycle states of a job-coordination row in {@code BATCH_JOB_COORDINATION}.
 *
 * <p>Normal flow is {@link #CREATED} &rarr; {@link #STARTED} &rarr; {@link #COMPLETED}. Recovery of a
 * job whose master node was lost moves it {@link #STARTED} &rarr; {@link #RECOVERING} (claimed by a
 * surviving node) &rarr; {@link #ABANDONED}. The {@code name()} of each value is the exact string
 * persisted in the {@code STATUS} column; this enum is the single source of truth for these values.</p>
 *
 * @author Janardhan Chejarla
 */
public enum CoordinationStatus {
    CREATED,
    STARTED,
    COMPLETED,
    RECOVERING,
    ABANDONED
}
