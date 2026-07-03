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
package io.github.jchejarla.springbatch.clustering.dialect;

import io.github.jchejarla.springbatch.clustering.core.DBSpecificQueryProvider;
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.MySQLDatabaseQueryProvider;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Validates the MariaDB dialect (shared with MySQL) against a real MariaDB engine.
 * Enable with {@code -Dcontainer.it=true}.
 */
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfSystemProperty(named = "container.it", matches = "true")
class MariaDbSchemaIT extends AbstractDialectSchemaIT {

    @Container
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.4");

    @Override
    JdbcDatabaseContainer<?> container() {
        return DB;
    }

    @Override
    DBSpecificQueryProvider queryProvider() {
        return new MySQLDatabaseQueryProvider();
    }

    @Override
    String springBatchSchema() {
        return "org/springframework/batch/core/schema-mariadb.sql";
    }

    @Override
    String clusterSchema() {
        return "schema/schema-mariadb.sql";
    }
}
