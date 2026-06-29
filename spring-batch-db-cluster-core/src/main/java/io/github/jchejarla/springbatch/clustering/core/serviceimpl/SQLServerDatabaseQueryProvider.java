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

import io.github.jchejarla.springbatch.clustering.core.DBSpecificQueryProvider;

/**
 * Query provider for Microsoft SQL Server.
 *
 * <p>Heartbeat age is computed with {@code DATEDIFF_BIG} (SQL Server 2016+), which returns a
 * {@code bigint} and therefore avoids the 32-bit overflow that {@code DATEDIFF(MILLISECOND, ...)}
 * hits after ~24 days.</p>
 *
 * @author Janardhan Chejarla
 */
public class SQLServerDatabaseQueryProvider implements DBSpecificQueryProvider {

    @Override
    public String getMarkNodesUnreachableQuery() {
        return "UPDATE batch_nodes set status = ? where status = ? and DATEDIFF_BIG(MILLISECOND, last_updated_time, SYSDATETIME()) >= ?";
    }

    @Override
    public String getDeleteNodesUnreachableQuery() {
        return "DELETE from batch_nodes where status = ? and DATEDIFF_BIG(MILLISECOND, last_updated_time, SYSDATETIME()) >= ?";
    }

    @Override
    public String getTimeStampColumnWithDiffInMillisToCurrentTime(String columnName) {
        return "DATEDIFF_BIG(MILLISECOND, " + columnName + ", SYSDATETIME())";
    }
}
