package examples.io.github.jchejarla.springbatch.clustering.simplejob;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

@Slf4j
public class LargeSumExecutionTask implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Executing MultiNode Task - START");
        StepExecution stepExecution =chunkContext.getStepContext().getStepExecution();
        ExecutionContext executionContext = stepExecution.getExecutionContext();
        long start = executionContext.getLong("start");
        long end = executionContext.getLong("end");
        //long result = (end- start +1) * (start + end) /2;
        long result = 0;
        for(long i=start; i<=end; i++) {
            result += i;
        }
        executionContext.putLong("result", result);
        log.info("Executing MultiNode Task - END");
        return RepeatStatus.FINISHED;
    }

}
