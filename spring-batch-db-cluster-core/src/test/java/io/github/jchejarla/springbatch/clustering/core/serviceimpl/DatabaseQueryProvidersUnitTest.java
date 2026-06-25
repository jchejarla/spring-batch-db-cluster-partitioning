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
