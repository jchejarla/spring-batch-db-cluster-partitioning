package examples.io.github.jchejarla.springbatch.clustering.advancedjob;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class CSVItemReaderConfig {

    @Bean("customerReader")
    @StepScope
    public FlatFileItemReader<Customer> customerReader(
            @Value("#{stepExecutionContext['startRow']}") long startIndex,
            @Value("#{stepExecutionContext['endRow']}") long endIndex,
            @Value("#{jobParameters['inputFile']}") String inputFile,
            LineMapper<Customer> lineMapper) {
        FlatFileItemReader<Customer> reader = new FlatFileItemReader<Customer>() {
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
