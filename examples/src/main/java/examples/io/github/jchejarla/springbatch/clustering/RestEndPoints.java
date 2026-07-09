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
package examples.io.github.jchejarla.springbatch.clustering;

import examples.io.github.jchejarla.springbatch.clustering.simplejob.SimpleJobConfig;
import io.github.jchejarla.springbatch.clustering.autoconfigure.BatchClusterProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class RestEndPoints {

    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;
    private final BatchClusterProperties batchClusterProperties;

    // The framework generates the node id (<prefix>-<uuid>) at startup; read it from there.
    private String nodeId;

    @PostConstruct
    void init() {
        this.nodeId = batchClusterProperties.getNodeId();
    }

    // Defaults point at the bundled CSV under examples/data and a writable
    // output directory under ./build. Override via query parameters if needed.
    private static final String DEFAULT_ETL_INPUT_FILE = "examples/data/customers_example_data.csv";
    private static final String DEFAULT_ETL_OUTPUT_DIR = "./build/etl-output";

    @GetMapping("/etljob/rows/{rows}")
    public ResponseEntity<String> etlJob(@PathVariable("rows") Long rows,
                                         @RequestParam(value = "inputFile", required = false) String inputFile,
                                         @RequestParam(value = "outputDir", required = false) String outputDir) {
        String resolvedInput = (inputFile != null) ? inputFile : DEFAULT_ETL_INPUT_FILE;
        String resolvedOutput = (outputDir != null) ? outputDir : DEFAULT_ETL_OUTPUT_DIR;

        // Make sure the output directory exists; StaxEventItemWriter won't create it.
        try {
            Files.createDirectories(Path.of(resolvedOutput));
        } catch (IOException ioe) {
            log.error("Could not create output directory {}", resolvedOutput, ioe);
            throw new RuntimeException("Could not create output directory: " + resolvedOutput, ioe);
        }

        System.out.println(">>> [" + nodeId + "] (master) received job request: etl-clustered-job"
                + " rows=" + rows + " input=" + resolvedInput + " output=" + resolvedOutput);

        JobParameters parameters = new JobParametersBuilder()
                .addString("RUN_TIME", LocalDateTime.now().toString(), true)
                .addLong("rows", rows)
                .addString("inputFile", resolvedInput)
                .addString("outputDir", resolvedOutput)
                .toJobParameters();
        Job job = applicationContext.getBean("etlClusteredJob", Job.class);
        try {
            long startTime = System.currentTimeMillis();
            JobExecution jobExecution = jobLauncher.run(job, parameters);
            long endTime = System.currentTimeMillis();
            System.out.println(">>> [" + nodeId + "] (master) etl job " + jobExecution.getId()
                    + " finished in " + (endTime - startTime) + " ms with status " + jobExecution.getStatus());
            String sb = "Job Id : " + jobExecution.getId() + " Completed in " + (endTime - startTime) + " milli seconds." + "\n" +
                    "Output: converted " + rows + " rows CSV data into XML format under " + resolvedOutput;
            return ResponseEntity.ok(sb);
        } catch(Exception e) {
            log.error("Exception occurred when launching the Job", e);
            throw new RuntimeException("Exception occurred when launching the Job", e);
        }
    }

    @GetMapping("/clusteredjob/addition/taskSize/{taskSize}/from/{from}/to/{to}")
    public ResponseEntity<String> startJobNewSolution(@PathVariable("taskSize") Long taskSize, @PathVariable("from") Long from, @PathVariable("to") Long to) {
        System.out.println(">>> [" + nodeId + "] (master) received job request: clustered-job"
                + " taskSize=" + taskSize + " range=" + from + ".." + to);
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
            long sumValue = simpleJobConfig.getSumAggregatorCallback().getSum().longValue();
            System.out.println(">>> [" + nodeId + "] (master) job " + jobExecution.getId()
                    + " finished in " + (endTime - startTime) + " ms; total sum=" + sumValue);
            String sb = "Job Id : " + jobExecution.getId() + " Completed in " + (endTime - startTime) + " milli seconds." + "\n" +
                    "Output: " + "sum of number from " + from + " to " + to + " is " + sumValue;
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
            String sb = "Job Id : " + jobExecution.getId() + " Completed in " + (endTime - startTime) + " milli seconds." + "\n" +
                    "Output: " + "sum of number from " + from + " to " + to + " is " +result;
            return ResponseEntity.ok(sb);
        } catch(Exception e) {
            log.error("Exception occurred when launching the Job", e);
            throw new RuntimeException("Exception occurred when launching the Job", e);
        }
    }

    @GetMapping("/msgchannel/addition/taskSize/{taskSize}/from/{from}/to/{to}")
    public ResponseEntity<String> msgChannelPartitionHandler(@PathVariable("taskSize") Long taskSize, @PathVariable("from") Long from, @PathVariable("to") Long to) {
        JobParameters parameters = new JobParametersBuilder()
                .addString("RUN_TIME", LocalDateTime.now().toString(), true)
                .addLong("taskSize", taskSize)
                .addLong("from", from)
                .addLong("to", to)
                .toJobParameters();
        Job job = applicationContext.getBean("rangeSumJob", Job.class);
        try {
            long startTime = System.currentTimeMillis();
            JobExecution jobExecution = jobLauncher.run(job, parameters);
            Long result = (Long) jobExecution.getExecutionContext().get("totalSum");
            long endTime = System.currentTimeMillis();
            String sb = "Job Id : " + jobExecution.getId() + " Completed in " + (endTime - startTime) + " milli seconds." + "\n" +
                    "Output: " + "sum of number from " + from + " to " + to + " is " +result;
            return ResponseEntity.ok(sb);
        } catch(Exception e) {
            log.error("Exception occurred when launching the Job", e);
            throw new RuntimeException("Exception occurred when launching the Job", e);
        }
    }


}
