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
package io.github.jchejarla.springbatch.clustering.core.serviceimpl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseQueryProvidersUnitTest {

    @Test
    public void testSqlServerUsesDateDiffBig() {
        SQLServerDatabaseQueryProvider provider = new SQLServerDatabaseQueryProvider();
        assertTrue(provider.getTimeStampColumnWithDiffInMillisToCurrentTime("last_updated_time").contains("DATEDIFF_BIG"));
        assertTrue(provider.getMarkNodesUnreachableQuery().toUpperCase().startsWith("UPDATE"));
        assertTrue(provider.getDeleteNodesUnreachableQuery().toUpperCase().startsWith("DELETE"));
        assertTrue(provider.getMarkNodesUnreachableQuery().contains("last_updated_time"));
    }

    @Test
    public void testDb2UsesDaysAndMidnightSeconds() {
        DB2DatabaseQueryProvider provider = new DB2DatabaseQueryProvider();
        String diff = provider.getTimeStampColumnWithDiffInMillisToCurrentTime("last_updated_time");
        assertTrue(diff.contains("DAYS"));
        assertTrue(diff.contains("MIDNIGHT_SECONDS"));
        assertTrue(provider.getMarkNodesUnreachableQuery().contains("last_updated_time"));
        assertTrue(provider.getDeleteNodesUnreachableQuery().contains("last_updated_time"));
    }
}
