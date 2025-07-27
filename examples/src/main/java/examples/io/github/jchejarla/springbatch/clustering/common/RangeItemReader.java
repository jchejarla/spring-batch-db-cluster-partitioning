package examples.io.github.jchejarla.springbatch.clustering.common;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class RangeItemReader implements ItemReader<Range> {

    private final Range range;
    private boolean read = false;

    public RangeItemReader(Range range) {
        this.range = range;
    }

    @Override
    public Range read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!read) {
            read = true;
            return range;
        }
        return null;
    }
}
