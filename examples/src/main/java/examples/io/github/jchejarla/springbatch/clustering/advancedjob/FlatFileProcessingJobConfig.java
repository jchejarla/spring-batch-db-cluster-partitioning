package examples.io.github.jchejarla.springbatch.clustering.advancedjob;


import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.context.annotation.Bean;

public class FlatFileProcessingJobConfig {

    private static final String JOB_NAME = "cscode_batch-clustered-flatfile_processing_job";

    @Bean
    public Job dataProcessingJob(JobBuilder jobBuilder) {
        return null;
    }
}
