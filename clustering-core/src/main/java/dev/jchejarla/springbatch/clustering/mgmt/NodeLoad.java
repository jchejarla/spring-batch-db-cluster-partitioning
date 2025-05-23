package dev.jchejarla.springbatch.clustering.mgmt;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;

@Getter
public enum NodeLoad {

    INST;
    AtomicLong currentLoad = new AtomicLong(0);

}
