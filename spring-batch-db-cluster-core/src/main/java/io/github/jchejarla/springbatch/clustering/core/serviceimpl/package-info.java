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
 * Per-database {@code DBSpecificQueryProvider} implementations.
 *
 * <p>Supports PostgreSQL, MySQL/MariaDB, Oracle, SQL Server, Db2, and H2. Only dialect-specific SQL
 * lives here — chiefly the heartbeat-age timestamp arithmetic and the unreachable-node sweeps; the
 * remaining queries are shared standard SQL defined on the provider interface.</p>
 */
package io.github.jchejarla.springbatch.clustering.core.serviceimpl;
