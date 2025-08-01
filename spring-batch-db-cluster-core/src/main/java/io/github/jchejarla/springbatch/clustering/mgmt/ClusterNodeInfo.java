package io.github.jchejarla.springbatch.clustering.mgmt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@RequiredArgsConstructor
public class ClusterNodeInfo {
    private final String nodeId;
    private String hostIdentifier;
    private Date startTime;
    private long currentLoad;
    private Date lastHeartbeatTime;
    private NodeStatus nodeStatus;
}
