package dev.jchejarla.springbatch.clustering.core.serviceimpl;

import dev.jchejarla.springbatch.clustering.core.DBSpecificQueryProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MySQLDatabaseQueryProvider implements DBSpecificQueryProvider {

    @Override
    public String getMarkNodesUnreachableQuery() {
        return "UPDATE batch_nodes set status = ? where status = ? and (TIMESTAMPDIFF(SECOND, last_updated_time, NOW()) * 1000) >= ?";
    }

    @Override
    public String getDeleteNodesUnreachableQuery() {
        return "DELETE from batch_nodes where status = ? and (TIMESTAMPDIFF(SECOND, last_updated_time, NOW()) * 1000) >= ?";
    }

    @Override
    public String getTimeStampColumnWithDiffInMillisToCurrentTime(String columnName) {
        return "(TIMESTAMPDIFF(SECOND, "+columnName+", NOW()) * 1000)";
    }
}
