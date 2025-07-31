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
    private Date startTime;
    private int currentLoad;
    private Date lastHeartbeatTime;
    private NodeStatus nodeStatus;
}
