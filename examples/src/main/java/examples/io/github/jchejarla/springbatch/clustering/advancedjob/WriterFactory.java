package examples.io.github.jchejarla.springbatch.clustering.advancedjob;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.oxm.xstream.XStreamMarshaller;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WriterFactory {

    @Bean
    @StepScope
    public StaxEventItemWriter<Customer> createWriter(@Value("#{stepExecutionContext['partitionId']}") String partitionId,
                                                      @Value("#{jobParameters['outputDir']}") String outputDir) {
        StaxEventItemWriter<Customer> writer = new StaxEventItemWriter<>();
        writer.setResource(new FileSystemResource(outputDir + "/customers-part-" + partitionId + ".xml"));
        writer.setRootTagName("customers");

        XStreamMarshaller marshaller = new XStreamMarshaller();
        Map<String, Class<?>> aliases = new HashMap<>();
        aliases.put("customer", Customer.class);
        marshaller.setAliases(aliases);

        writer.setMarshaller(marshaller);
        return writer;
    }
}

