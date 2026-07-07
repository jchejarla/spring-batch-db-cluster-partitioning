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
package examples.io.github.jchejarla.springbatch.clustering.common;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

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
