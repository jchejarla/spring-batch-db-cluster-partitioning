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
 * The database-backed coordination core.
 *
 * <p>{@code DatabaseBackedClusterService} performs all coordination reads and writes — node
 * registration, heartbeats, partition lifecycle transitions, orphan detection, and master-job
 * recovery — through a {@code DBSpecificQueryProvider}, which isolates the small amount of
 * dialect-specific SQL from the shared standard SQL.</p>
 */
package io.github.jchejarla.springbatch.clustering.core;
