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
        return "((DAYS(CURRENT TIMESTAMP) - DAYS(" + columnName + ")) * 86400 + "
                + "(MIDNIGHT_SECONDS(CURRENT TIMESTAMP) - MIDNIGHT_SECONDS(" + columnName + "))) * 1000";
    }
}
