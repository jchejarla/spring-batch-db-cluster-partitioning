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
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.infrastructure.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class CSVItemReaderConfig {

    // --8<-- [start:etl-reader]
    @Bean("customerReader")
    @StepScope   // one reader per partition, so it can read that partition's row window
    public FlatFileItemReader<Customer> customerReader(
            @Value("#{stepExecutionContext['startRow']}") long startIndex,
            @Value("#{stepExecutionContext['endRow']}") long endIndex,
            @Value("#{jobParameters['inputFile']}") String inputFile,
            LineMapper<Customer> lineMapper) {
        FlatFileItemReader<Customer> reader = new FlatFileItemReader<Customer>(lineMapper) {
            private int currentLine = 0;

            @Override
            public Customer read() throws Exception {
                Customer customer;
                while ((customer = super.read()) != null) {
                    currentLine++;
                    if (currentLine < startIndex) {
                        continue;
                    }
                    if (currentLine > endIndex) {
                        return null;
                    }
                    return customer;
                }
                return null;
            }
        };

        reader.setResource(new FileSystemResource(inputFile));
        reader.setLinesToSkip(1); // Skip header
        reader.setLineMapper(lineMapper);

        return reader;
    }
    // --8<-- [end:etl-reader]

    @Bean
    public LineMapper<Customer> lineMapper() {
        DefaultLineMapper<Customer> mapper = new DefaultLineMapper<Customer>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("customer_id", "first_name", "last_name", "email", "signup_date");

        BeanWrapperFieldSetMapper<Customer> fieldSetMapper = new BeanWrapperFieldSetMapper<Customer>();
        fieldSetMapper.setTargetType(Customer.class);

        mapper.setLineTokenizer(tokenizer);
        mapper.setFieldSetMapper(fieldSetMapper);
        return mapper;
    }

}
