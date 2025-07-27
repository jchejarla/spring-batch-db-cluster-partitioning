package examples.io.github.jchejarla.springbatch.clustering.advancedjob;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class FlatFileWriter implements ItemWriter<String> {

    @Override
    public void write(Chunk<? extends String> chunk) throws Exception {

    }
}
