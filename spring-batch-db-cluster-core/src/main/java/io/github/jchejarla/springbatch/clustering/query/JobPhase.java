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
 * Master-side coordination phases recorded (when enabled) to {@code BATCH_JOB_PHASE_EVENTS}.
 *
 * <p>The gaps between consecutive phases give the framework-specific timings that Spring Batch's own
 * tables do not capture: {@code RECEIVED}&rarr;{@code PARTITIONED} = partitioning time,
 * {@code PARTITIONED}&rarr;{@code DISTRIBUTED} = time to persist the partitions, and
 * {@code DISTRIBUTED}&rarr;{@code COMPLETION_DETECTED} = distributed execution + completion detection.
 * The {@code name()} of each value is the exact string persisted.</p>
 *
 * @author Janardhan Chejarla
 */
public enum JobPhase {
    RECEIVED,
    PARTITIONED,
    DISTRIBUTED,
    COMPLETION_DETECTED
}
