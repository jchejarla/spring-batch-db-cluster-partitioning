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
package io.github.jchejarla.springbatch.clustering.autoconfigure;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.beans.factory.InitializingBean;

/**
 * Fails fast at startup when clustering is enabled but the active {@link JobRepository} is the in-memory
 * {@link ResourcelessJobRepository} — the Spring Batch 6 default.
 *
 * <p>Distributed partitioning coordinates through <em>shared, persisted</em> job metadata: the master
 * persists partition step executions, and worker nodes on other JVMs read them back from the same
 * repository to run them. The resourceless repository keeps everything in the heap of a single JVM, so a
 * cluster silently cannot work on it. Rather than let jobs fail deep inside execution with a confusing
 * foreign-key error, this validator stops the context with an actionable message.</p>
 */
@RequiredArgsConstructor
public class ClusterJobRepositoryValidator implements InitializingBean {

    private final JobRepository jobRepository;

    @Override
    public void afterPropertiesSet() {
        if (jobRepository instanceof ResourcelessJobRepository) {
            throw new BatchConfigurationException(
                    "Spring Batch clustering is enabled (spring.batch.cluster.enabled=true) but the active " +
                    "JobRepository is the in-memory ResourcelessJobRepository, which is the Spring Batch 6 " +
                    "default. Distributed partitioning requires a JDBC-backed JobRepository so cluster nodes " +
                    "can share job metadata. Configure one with @EnableBatchProcessing + " +
                    "@EnableJdbcJobRepository(dataSourceRef = \"dataSource\"), or by defining a JDBC " +
                    "JobRepository bean (e.g. via JdbcJobRepositoryFactoryBean).");
        }
    }
}
