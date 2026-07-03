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
import io.github.jchejarla.springbatch.clustering.core.serviceimpl.DB2DatabaseQueryProvider;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Validates the Db2 dialect against a real Db2 engine. The Db2 image is large and slow to start, so it
 * is gated on its own flag: enable with {@code -Dcontainer.it.db2=true}.
 */
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfSystemProperty(named = "container.it.db2", matches = "true")
class Db2SchemaIT extends AbstractDialectSchemaIT {

    @Container
    static final Db2Container DB =
            new Db2Container("icr.io/db2_community/db2:11.5.9.0").acceptLicense();

    @Override
    JdbcDatabaseContainer<?> container() {
        return DB;
    }

    @Override
    DBSpecificQueryProvider queryProvider() {
        return new DB2DatabaseQueryProvider();
    }

    @Override
    String springBatchSchema() {
        return "org/springframework/batch/core/schema-db2.sql";
    }

    @Override
    String clusterSchema() {
        return "schema/schema-db2.sql";
    }
}
