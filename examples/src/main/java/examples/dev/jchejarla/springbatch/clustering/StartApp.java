package examples.dev.jchejarla.springbatch.clustering;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@EnableBatchProcessing
public class StartApp {

    public static void main(String[] args) {

        try {
            SpringApplication.run(StartApp.class ,args);
        } catch (Exception e) {
           log.error("Exception occurred when starting the application", e);
        }

    }
}
