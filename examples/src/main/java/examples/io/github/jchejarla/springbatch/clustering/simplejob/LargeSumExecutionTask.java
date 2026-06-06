package examples.io.github.jchejarla.springbatch.clustering.simplejob;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class LargeSumExecutionTask implements Tasklet {

    private final String nodeId;

    public LargeSumExecutionTask(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
        ExecutionContext executionContext = stepExecution.getExecutionContext();
        long start = executionContext.getLong("start");
        long end = executionContext.getLong("end");
        String stepName = stepExecution.getStepName();
        System.out.println(">>> [" + nodeId + "] (worker) picked up partition "
                + stepName + " (range " + start + ".." + end + ")");
        log.info("Executing MultiNode Task - START");
        long result = 0;
        for(long i=start; i<=end; i++) {
            result += i;
        }
        executionContext.putLong("result", result);
        System.out.println(">>> [" + nodeId + "] (worker) completed partition "
                + stepName + " result=" + result);
        log.info("Executing MultiNode Task - END");
        return RepeatStatus.FINISHED;
    }

}
