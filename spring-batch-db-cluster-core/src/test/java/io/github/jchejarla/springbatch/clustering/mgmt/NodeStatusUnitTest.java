package io.github.jchejarla.springbatch.clustering.mgmt;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NodeStatusUnitTest extends BaseUnitTest {

    @Test
    public void testNodeStatus() {
        NodeStatus nodeStatus = NodeStatus.ACTIVE;
        assertEquals(NodeStatus.ACTIVE, nodeStatus);
        nodeStatus = NodeStatus.UNREACHABLE;
        assertEquals(NodeStatus.UNREACHABLE, nodeStatus);
    }
}
