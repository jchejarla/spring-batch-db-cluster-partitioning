package io.github.jchejarla.springbatch.clustering.core.serviceimpl;

import io.github.jchejarla.springbatch.clustering.core.DBSpecificQueryProvider;

public class H2DatabaseQueryProvider implements DBSpecificQueryProvider {

    @Override
    public String getMarkNodesUnreachableQuery() {
        return "UPDATE batch_nodes set status = ? where status = ? and (EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - LAST_UPDATED_TIME)) * 1000) >= ?";
    }

    @Override
    public String getDeleteNodesUnreachableQuery() {
        return "DELETE from batch_nodes where status = ? and (EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - LAST_UPDATED_TIME)) * 1000) >= ?";
    }

    @Override
    public String getTimeStampColumnWithDiffInMillisToCurrentTime(String columnName) {
        return "(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - "+columnName+")) * 1000)";
    }
}
