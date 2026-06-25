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
