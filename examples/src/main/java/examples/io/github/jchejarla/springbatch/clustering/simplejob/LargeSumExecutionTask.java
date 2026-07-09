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
package examples.io.github.jchejarla.springbatch.clustering.simplejob;

import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

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
                + stepName + " (range " + start + ".." + end + ") on "
                + (Thread.currentThread().isVirtual() ? "virtual" : "platform") + " thread "
                + Thread.currentThread());
        long result = 0;
        for(long i=start; i<=end; i++) {
            result += i;
        }
        executionContext.putLong("result", result);
        System.out.println(">>> [" + nodeId + "] (worker) completed partition "
                + stepName + " result=" + result);
        return RepeatStatus.FINISHED;
    }

}
