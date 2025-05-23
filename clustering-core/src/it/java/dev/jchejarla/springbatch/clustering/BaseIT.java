package dev.jchejarla.springbatch.clustering;

import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { BatchTestApplication.class, BatchTestConfig.class },
                    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Import(BatchTestConfig.class)
public abstract class BaseIT {

    @Autowired
    protected JobLauncherTestUtils jobLauncherTestUtils;

}
