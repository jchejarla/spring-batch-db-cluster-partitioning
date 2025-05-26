package examples.dev.jchejarla.springbatch.clustering.taskexec;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@RequiredArgsConstructor
public class RangeSumWriter implements ItemWriter<Long> {

    private StepExecution stepExecution;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }


    @Override
    public void write(Chunk<? extends Long> sums) throws Exception {
        if (!sums.isEmpty()) {
            stepExecution.getExecutionContext().putLong("partitionSum", sums.getItems().get(0));
        }

        //ExecutionContext stepContext = stepExecution.getExecutionContext();
        //stepContext.putLong("partialSum", processor.getTotal());
    }

}
