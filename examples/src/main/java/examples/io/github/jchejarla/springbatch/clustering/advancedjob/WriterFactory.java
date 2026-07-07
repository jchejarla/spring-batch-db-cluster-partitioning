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
package examples.io.github.jchejarla.springbatch.clustering.advancedjob;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.oxm.xstream.XStreamMarshaller;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WriterFactory {

    @Bean(destroyMethod = "")  // Spring Batch already closes the writer
    @StepScope
    public StaxEventItemWriter<Customer> createWriter(@Value("#{stepExecutionContext['partitionId']}") String partitionId,
                                                      @Value("#{jobParameters['outputDir']}") String outputDir) {
        // Spring Batch 6 removed the no-arg StaxEventItemWriter constructor; the marshaller is now required
        // up front, so build it first.
        XStreamMarshaller marshaller = new XStreamMarshaller();
        Map<String, Class<?>> aliases = new HashMap<>();
        aliases.put("customer", Customer.class);
        marshaller.setAliases(aliases);

        StaxEventItemWriter<Customer> writer = new StaxEventItemWriter<Customer>(marshaller);
        writer.setResource(new FileSystemResource(outputDir + "/customers-part-" + partitionId + ".xml"));
        writer.setRootTagName("customers");
        return writer;
    }
}

