package io.github.jchejarla.springbatch.clustering.api;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

public class MultiNodeTask implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        StepExecution stepExecution =chunkContext.getStepContext().getStepExecution();
        ExecutionContext executionContext = stepExecution.getExecutionContext();
        Integer start = executionContext.getInt("start");
        Integer end = executionContext.getInt("end");
        long result = (long)(end- start +1) * (start + end) /2;
        executionContext.put("result", result);
        return RepeatStatus.FINISHED;
    }

}