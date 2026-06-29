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
package io.github.jchejarla.springbatch.clustering.api;

import io.github.jchejarla.springbatch.clustering.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.Objects;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(SimpleAdditionTestJobConfig.class)
public class SimpleAdditionAggrJobIT extends BaseIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Autowired
    SimpleAdditionTestJobConfig testJobConfig;

    @Autowired
    @Qualifier("clusteredJob")
    private Job clusteredJob;

    @Test
    void jobShouldCompleteSuccessfully() throws Exception {
        await().atMost(Duration.ofMillis(5000)).until(() -> {
            Integer nodeCount = jdbcTemplate.queryForObject("select count(*) from batch_nodes", Integer.class);
            return Objects.nonNull(nodeCount) && nodeCount > 0;
        });

        jobLauncherTestUtils.setJob(clusteredJob);
        JobExecution execution = jobLauncherTestUtils.launchJob();
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        long calculatedSum = testJobConfig.getSimpleSumAggregatorCallback().getSum().longValue();
        long expected = (1000 * (1000 +1)) /2;
        assertEquals(expected, calculatedSum);
    }

}
