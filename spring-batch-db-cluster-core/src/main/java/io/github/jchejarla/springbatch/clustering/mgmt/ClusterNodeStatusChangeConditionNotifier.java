package io.github.jchejarla.springbatch.clustering.mgmt;

import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;

@ConditionalOnClusterEnabled
public interface ClusterNodeStatusChangeConditionNotifier {
     void onClusterNodeHeartbeatFail();
}
