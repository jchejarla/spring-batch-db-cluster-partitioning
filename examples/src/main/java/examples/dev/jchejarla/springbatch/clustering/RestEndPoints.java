package examples.dev.jchejarla.springbatch.clustering;

import examples.dev.jchejarla.springbatch.clustering.simplejob.SimpleJobConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class RestEndPoints {

    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;

    @GetMapping("/clusteredjob/addition/taskSize/{taskSize}/from/{from}/to/{to}")
    public ResponseEntity<String> startJobNewSolution(@PathVariable("taskSize") Long taskSize, @PathVariable("from") Long from, @PathVariable("to") Long to) {
        JobParameters parameters = new JobParametersBuilder()
                .addString("RUN_TIME", LocalDateTime.now().toString(), true)
                .addLong("taskSize", taskSize)
                .addLong("from", from)
                .addLong("to", to)
                .toJobParameters();
        Job job = applicationContext.getBean("clusteredJob", Job.class);
        try {
            long startTime = System.currentTimeMillis();
            JobExecution jobExecution = jobLauncher.run(job, parameters);
            long endTime = System.currentTimeMillis();
            SimpleJobConfig simpleJobConfig = applicationContext.getBean(SimpleJobConfig.class);
            String sb = "Job Id : " + jobExecution.getJobId() + " Completed in " + (endTime - startTime) + " milli seconds." + "\n" +
                    "Output: " + "sum of number from " + from + " to " + to + " is "+
                    simpleJobConfig.getSumAggregatorCallback().getSum();
            return ResponseEntity.ok(sb);
        } catch(Exception e) {
            log.error("Exception occurred when launching the Job", e);
            throw new RuntimeException("Exception occurred when launching the Job", e);
        }
    }

    @GetMapping("/singlenodejob/addition/taskSize/{taskSize}/from/{from}/to/{to}")
    public ResponseEntity<String> startJobDefaultTaskPartitionHandler(@PathVariable("taskSize") Long taskSize, @PathVariable("from") Long from, @PathVariable("to") Long to) {
        JobParameters parameters = new JobParametersBuilder()
                .addString("RUN_TIME", LocalDateTime.now().toString(), true)
                .addLong("taskSize", taskSize)
                .addLong("from", from)
                .addLong("to", to)
                .toJobParameters();
        Job job = applicationContext.getBean("rangeSumSingleNodeJob", Job.class);
        try {
            long startTime = System.currentTimeMillis();
            JobExecution jobExecution = jobLauncher.run(job, parameters);
            Long result = (Long) jobExecution.getExecutionContext().get("totalSum");
            long endTime = System.currentTimeMillis();
            String sb = "Job Id : " + jobExecution.getJobId() + " Completed in " + (endTime - startTime) + " milli seconds." + "\n" +
                    "Output: " + "sum of number from " + from + " to " + to + " is " +result;
            return ResponseEntity.ok(sb);
        } catch(Exception e) {
            log.error("Exception occurred when launching the Job", e);
            throw new RuntimeException("Exception occurred when launching the Job", e);
        }
    }

}
