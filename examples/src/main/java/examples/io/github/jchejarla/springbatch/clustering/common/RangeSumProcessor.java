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

import org.springframework.batch.item.ItemProcessor;

public class RangeSumProcessor implements ItemProcessor<Range, Long> {

    @Override
    public Long process(Range range) throws Exception {
        long sum = 0;
        for (long i = range.getStart(); i <= range.getEnd(); i++) {
            sum += i;
        }
        return sum;
    }

}
