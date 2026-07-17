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
 * Query provider for IBM Db2.
 *
 * <p>Db2's {@code TIMESTAMPDIFF} is only an approximation, so heartbeat age is computed exactly to the
 * second using {@code DAYS()} and {@code MIDNIGHT_SECONDS()}, then scaled to milliseconds. This matches
 * the second-granularity already used by the MySQL and Oracle providers (the heartbeat thresholds are
 * in seconds, so sub-second precision is not required).</p>
 *
 * @author Janardhan Chejarla
 */
public class DB2DatabaseQueryProvider implements DBSpecificQueryProvider {

    // Db2's age comparison reads the CURRENT TIMESTAMP special register (two words); write with the same
    // register so timestamps and comparisons share one clock.
    @Override
    public String currentDbTimestampExpression() {
        return "CURRENT TIMESTAMP";
    }

    @Override
    public String getMarkNodesUnreachableQuery() {
        return "UPDATE batch_nodes set status = ? where status = ? and " + diffInMillis("last_updated_time") + " >= ?";
    }

    @Override
    public String getDeleteNodesUnreachableQuery() {
        return "DELETE from batch_nodes where status = ? and " + diffInMillis("last_updated_time") + " >= ?";
    }

    @Override
    public String getTimeStampColumnWithDiffInMillisToCurrentTime(String columnName) {
        return diffInMillis(columnName);
    }

    private String diffInMillis(String columnName) {
        // Use the CURRENT_TIMESTAMP scalar-function form (underscore) here, not the two-word "CURRENT
        // TIMESTAMP" special register: Db2 accepts the register as a bare value but rejects it as a
        // function argument (DAYS(CURRENT TIMESTAMP) is a syntax error). Both resolve to the same clock,
        // so this stays consistent with the timestamps written by currentDbTimestampExpression().
        return "((DAYS(CURRENT_TIMESTAMP) - DAYS(" + columnName + ")) * 86400 + "
                + "(MIDNIGHT_SECONDS(CURRENT_TIMESTAMP) - MIDNIGHT_SECONDS(" + columnName + "))) * 1000";
    }
}
