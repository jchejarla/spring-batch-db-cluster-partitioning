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
 * Cross-database validation of the per-dialect DDL ({@code schema-*.sql}) and the
 * {@code DBSpecificQueryProvider} query surface against real database engines via Testcontainers.
 *
 * <p>These integration tests are opt-in because they pull sizeable database images and need a Docker
 * daemon. They are skipped (not failed) when Docker is unavailable.</p>
 *
 * <ul>
 *   <li>{@code mvn verify -Dcontainer.it=true} — runs the MariaDB and SQL Server checks.</li>
 *   <li>{@code mvn verify -Dcontainer.it.db2=true} — additionally runs the Db2 check (large, slow image).</li>
 * </ul>
 *
 * <p>Note: the SQL Server and Db2 images are published for amd64 only; on an arm64 host they run under
 * emulation and may time out. They pass on the amd64 CI runners.</p>
 */
package io.github.jchejarla.springbatch.clustering.dialect;
