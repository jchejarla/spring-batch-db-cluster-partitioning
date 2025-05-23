package dev.jchejarla.springbatch.clustering.mgmt;

import dev.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class ClusterNodeUnitTest extends BaseUnitTest {

    @Test
    public void testClusterNodeRecord() {
        String nodeId = UUID.randomUUID().toString();
        ClusterNode clusterNode1 = new ClusterNode(nodeId, 0);
        ClusterNode clusterNode2 = new ClusterNode(nodeId, 0);
        Assertions.assertEquals(clusterNode1, clusterNode2);
        clusterNode2 = new ClusterNode(UUID.randomUUID().toString(), 0);
        Assertions.assertNotEquals(clusterNode1, clusterNode2);
    }
}
