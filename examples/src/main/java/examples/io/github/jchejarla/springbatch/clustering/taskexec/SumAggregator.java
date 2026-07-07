/*
 * Copyright 2025 Janardhan Chejarla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package examples.io.github.jchejarla.springbatch.clustering.taskexec;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.infrastructure.item.ExecutionContext;

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
