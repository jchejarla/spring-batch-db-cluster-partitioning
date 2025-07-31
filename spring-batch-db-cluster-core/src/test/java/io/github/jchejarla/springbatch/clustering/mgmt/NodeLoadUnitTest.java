package io.github.jchejarla.springbatch.clustering.mgmt;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NodeLoadUnitTest extends BaseUnitTest {

    @Test
    public void testNodeLoadEnum() {
        NodeLoad nodeLoad = NodeLoad.INST;
        nodeLoad.currentLoad.set(5);
        assertEquals(5, nodeLoad.getCurrentLoad());
    }
}
