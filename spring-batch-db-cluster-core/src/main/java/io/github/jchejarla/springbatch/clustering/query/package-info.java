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
 * Read-only, job-centric observability of the cluster.
 *
 * <p>{@code BatchClusterQueryService} answers "given a job, how many partitions, where are they running,
 * and in what state" via plain {@code SELECT}s over the coordination tables. It is the foundation the
 * actuator job endpoint wraps, and that applications can call directly to build their own dashboards.</p>
 */
package io.github.jchejarla.springbatch.clustering.query;
