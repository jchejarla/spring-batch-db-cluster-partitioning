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
