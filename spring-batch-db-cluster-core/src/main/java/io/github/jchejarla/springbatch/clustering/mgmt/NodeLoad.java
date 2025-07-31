package io.github.jchejarla.springbatch.clustering.mgmt;

import java.util.concurrent.atomic.AtomicLong;

public enum NodeLoad {

    INST;
    AtomicLong currentLoad = new AtomicLong(0);

    public long incrementLoadCount() {
        return currentLoad.incrementAndGet();
    }

    public long decrementLoadCount() {
        if(getCurrentLoad() == 0) {
            return 0;
        }
        return currentLoad.decrementAndGet();
    }

    public long getCurrentLoad() {
        return currentLoad.get();
    }
}
