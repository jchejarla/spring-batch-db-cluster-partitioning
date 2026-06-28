package io.github.jchejarla.springbatch.clustering.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DatabaseDriver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClusterSchemaLocationUnitTest {

    @Test
    public void testEverySupportedDriverMapsToAnExistingSchemaResource() {
        DatabaseDriver[] supported = {
                DatabaseDriver.POSTGRESQL, DatabaseDriver.MYSQL, DatabaseDriver.MARIADB,
                DatabaseDriver.ORACLE, DatabaseDriver.SQLSERVER, DatabaseDriver.DB2, DatabaseDriver.H2
        };
        for (DatabaseDriver driver : supported) {
            String location = BatchClusterAutoConfiguration.clusterSchemaLocation(driver);
            assertTrue(location.startsWith("classpath:schema/schema-"), "unexpected location: " + location);
            String resource = location.substring("classpath:".length());
            assertNotNull(getClass().getClassLoader().getResource(resource),
                    "missing bundled schema resource for " + driver + ": " + resource);
        }
    }
}
