package examples.io.github.jchejarla.springbatch.clustering;

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
            SpringApplication app = new SpringApplication(StartApp.class);
            // Demo-only: when the h2 profile is active and the user supplied a
            // fixed --spring.batch.cluster.node-id, append a short hex suffix so
            // consecutive restarts do not collide on BATCH_NODES. Inactive for
            // postgres/mysql/oracle profiles.
            app.addListeners(new H2NodeIdSuffixListener());
            app.run(args);
        } catch (Exception e) {
           log.error("Exception occurred when starting the application", e);
        }

    }
}
