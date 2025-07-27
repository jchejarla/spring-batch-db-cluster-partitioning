package examples.io.github.jchejarla.springbatch.clustering.taskexec;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;

public class SumAggregator implements StepExecutionListener {
    private long totalSum = 0;

    @Override
    public void beforeStep(StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        for (StepExecution child : stepExecution.getJobExecution().getStepExecutions()) {
            ExecutionContext context = child.getExecutionContext();
            if (context.containsKey("partitionSum")) {
                totalSum += context.getLong("partitionSum");
            }
        }
        stepExecution.getJobExecution().getExecutionContext().putLong("totalSum", totalSum);
        System.out.println("TOTAL SUM: " + totalSum);
        return ExitStatus.COMPLETED;
    }
}
