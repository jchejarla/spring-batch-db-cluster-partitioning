package examples.dev.jchejarla.springbatch.clustering.simplejob;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class MultiNodeExecutionTask implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Executing MultiNode Task - START");
        StepExecution stepExecution =chunkContext.getStepContext().getStepExecution();
        ExecutionContext executionContext = stepExecution.getExecutionContext();
        Long start = executionContext.getLong("start");
        Long end = executionContext.getLong("end");
        long result = (end- start +1) * (start + end) /2;
        executionContext.put("result", result);
        log.info("Executing MultiNode Task - END");
        return RepeatStatus.FINISHED;
    }

}
