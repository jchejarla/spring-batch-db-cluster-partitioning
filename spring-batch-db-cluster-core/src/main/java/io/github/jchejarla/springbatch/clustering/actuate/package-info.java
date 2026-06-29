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
 * Spring Boot Actuator integration for observability of the batch cluster.
 *
 * <p>Provides a {@code /actuator/batch-cluster} endpoint exposing nodes and their partition
 * assignments, health indicators for overall cluster and per-node liveness, and an info contributor
 * that reports the active clustering configuration.</p>
 */
package io.github.jchejarla.springbatch.clustering.actuate;
