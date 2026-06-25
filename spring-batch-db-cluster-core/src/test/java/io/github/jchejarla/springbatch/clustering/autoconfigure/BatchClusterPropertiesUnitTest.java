package io.github.jchejarla.springbatch.clustering.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BatchClusterPropertiesUnitTest {

    @Test
    public void testNodeIdUsesConfiguredPrefix() {
        BatchClusterProperties props = new BatchClusterProperties();
        props.setNodeIdPrefix("worker");
        props.resolveNodeId();
        assertTrue(props.getNodeId().startsWith("worker-"));
        assertTrue(props.getNodeId().length() > "worker-".length(), "a unique suffix should be appended");
    }

    @Test
    public void testNodeIdDefaultsToHostNameWhenNoPrefix() {
        BatchClusterProperties props = new BatchClusterProperties();
        props.resolveNodeId();
        assertNotNull(props.getNodeId());
        assertTrue(props.getNodeId().contains("-"), "id should be <prefix>-<uuid>");
    }

    @Test
    public void testNodeIdIsUniquePerResolution() {
        BatchClusterProperties a = new BatchClusterProperties();
        a.setNodeIdPrefix("worker");
        a.resolveNodeId();
        BatchClusterProperties b = new BatchClusterProperties();
        b.setNodeIdPrefix("worker");
        b.resolveNodeId();
        assertNotEquals(a.getNodeId(), b.getNodeId());
    }
}
