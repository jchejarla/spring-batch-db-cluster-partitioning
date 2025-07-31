package io.github.jchejarla.springbatch.clustering.partition;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PartitionTransferableProp {
    YES (ClusterPartitioningConstants.IS_TRANSFERABLE, Boolean.TRUE),
    NO (ClusterPartitioningConstants.IS_TRANSFERABLE, Boolean.FALSE);
    final String key;
    final Boolean val;
}
