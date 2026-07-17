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
 * <p>Heartbeat age is computed with {@code TIMESTAMPDIFF} over the timestamp difference and cast to
 * {@code BIGINT} before scaling to milliseconds. {@code TIMESTAMPDIFF} is exact for the sub-month
 * durations the heartbeat/cleanup thresholds compare (it only estimates month/year spans, which never
 * occur here), and the {@code BIGINT} cast prevents the 32-bit overflow that {@code seconds * 1000}
 * would otherwise hit for large thresholds.</p>
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
        // Elapsed milliseconds between columnName and now = seconds (TIMESTAMPDIFF over the timestamp
        // difference) * 1000, cast to BIGINT first so it can't overflow a 32-bit INTEGER. CHAR(...) is
        // required: TIMESTAMPDIFF's second argument is the 22-char string form of a timestamp duration.
        return "BIGINT(TIMESTAMPDIFF(2, CHAR(CURRENT_TIMESTAMP - " + columnName + "))) * 1000";
    }
}
