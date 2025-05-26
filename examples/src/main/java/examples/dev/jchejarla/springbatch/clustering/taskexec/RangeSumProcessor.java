package examples.dev.jchejarla.springbatch.clustering.taskexec;

import org.springframework.batch.item.ItemProcessor;

public class RangeSumProcessor implements ItemProcessor<Range, Long> {

    @Override
    public Long process(Range range) throws Exception {
        long sum = 0;
        for (long i = range.getStart(); i <= range.getEnd(); i++) {
            sum += i;
        }
        return sum;
    }

}
