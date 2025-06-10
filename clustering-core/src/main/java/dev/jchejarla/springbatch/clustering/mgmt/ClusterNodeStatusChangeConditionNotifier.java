package dev.jchejarla.springbatch.clustering.mgmt;

import dev.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;

@ConditionalOnClusterEnabled
public interface ClusterNodeStatusChangeConditionNotifier {
     void onClusterNodeHeartbeatFail();
}
