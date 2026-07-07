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
package examples.io.github.jchejarla.springbatch.clustering;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Spring Batch 6 defaults to an in-memory ResourcelessJobRepository; a cluster needs a JDBC-backed
// repository so nodes share job metadata. @EnableJdbcJobRepository selects the JDBC repository.
@Slf4j
@SpringBootApplication
@EnableJdbcJobRepository
public class StartApp {

    public static void main(String[] args) {

        try {
            SpringApplication app = new SpringApplication(StartApp.class);
            app.run(args);
        } catch (Exception e) {
           log.error("Exception occurred when starting the application", e);
        }

    }
}
