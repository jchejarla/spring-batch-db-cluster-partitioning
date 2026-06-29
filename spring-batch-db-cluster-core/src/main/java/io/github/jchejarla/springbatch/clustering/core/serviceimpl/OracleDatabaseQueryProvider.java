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

public class OracleDatabaseQueryProvider implements DBSpecificQueryProvider {
    @Override
    public String getMarkNodesUnreachableQuery() {
        return "UPDATE batch_nodes set status = ? where status = ? and ((CAST(SYSTIMESTAMP AS DATE) - CAST(LAST_UPDATED_TIME AS DATE)) * 24 * 60 * 60 * 1000) >= ?";
    }

    @Override
    public String getDeleteNodesUnreachableQuery() {
        return "DELETE from batch_nodes where status = ? and ((CAST(SYSTIMESTAMP AS DATE) - CAST(LAST_UPDATED_TIME AS DATE)) * 24 * 60 * 60 * 1000) >= ?";
    }

    @Override
    public String getTimeStampColumnWithDiffInMillisToCurrentTime(String columnName) {
        return "((CAST(SYSTIMESTAMP AS DATE) - CAST("+columnName+" AS DATE)) * 24 * 60 * 60 * 1000)";
    }
}
