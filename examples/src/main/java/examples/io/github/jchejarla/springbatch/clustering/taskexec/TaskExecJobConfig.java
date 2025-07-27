package examples.io.github.jchejarla.springbatch.clustering.taskexec;

import examples.io.github.jchejarla.springbatch.clustering.common.Range;
import examples.io.github.jchejarla.springbatch.clustering.common.RangeItemReader;
import examples.io.github.jchejarla.springbatch.clustering.common.RangeSumProcessor;
import examples.io.github.jchejarla.springbatch.clustering.common.RangeSumWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableBatchProcessing
@ConditionalOnProperty(value = "spring.batch.single-node.enabled", havingValue = "true")
public class TaskExecJobConfig {


    @Autowired
    Partitioner partitioner;

    @Bean
    @StepScope
    public RangeItemReader reader(@Value("#{stepExecutionContext['start']}") Long start, @Value("#{stepExecutionContext['end']}") Long end) {
        return new RangeItemReader(new Range(start, end));
    }

    @Bean
    @StepScope
    public RangeSumProcessor processor() {
        return new RangeSumProcessor();
    }

    @Bean
    @StepScope
    public RangeSumWriter writer() {
        return new RangeSumWriter();
    }

    @Bean
    public Step partitionStep(JobRepository jobRepository, PlatformTransactionManager txnManager) {
        return new StepBuilder("partitionStep", jobRepository)
                .<Range, Long>chunk(1, txnManager)
                .reader(reader(null, null))
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    @StepScope
    public Partitioner rangePartitioner(@Value("#{jobParameters['taskSize']}") Long tasksSize, @Value("#{jobParameters['from']}") Long from, @Value("#{jobParameters['to']}") Long to) {
        return gridSize -> {
            Map<String, ExecutionContext> partitions = new HashMap<>();

            long totalNumbers = to - from + 1;
            long partitionSize = totalNumbers / tasksSize;
            long remainder = totalNumbers % tasksSize;
            long currentStart = from;
            for (int i=0; i<tasksSize; i++) {
                ExecutionContext context = new ExecutionContext();
                context.putString("partitionKey", "partition-" + i);
                long currentEnd = currentStart + partitionSize - 1;

                if (remainder > 0) {
                    currentEnd++;
                    remainder--;
                }

                currentEnd = Math.min(currentEnd, to);
                context.putLong("start", currentStart);
                context.putLong("end", currentEnd);
                partitions.put("partition" + i, context);
                currentStart = currentEnd +1;
            }

            return partitions;
        };
    }

    @Bean
    public Step masterStep(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder("partitionStep", jobRepository)
                .partitioner("partitionStep", partitioner)
                .step(partitionStep(jobRepository, platformTransactionManager))
                .listener(new SumAggregator())
                .taskExecutor(taskExecutor()).build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(10);
        return taskExecutor;
    }

    @Bean("rangeSumSingleNodeJob")
    public Job rangeSumJob(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new JobBuilder("rangeSumJob-singleNode", jobRepository)
                .start(masterStep(jobRepository, platformTransactionManager))
                .listener(jobResultListener())
                .build();
    }

    @Bean
    public JobExecutionListener jobResultListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {}

            @Override
            public void afterJob(JobExecution jobExecution) {
                Long totalSum = jobExecution.getExecutionContext().getLong("totalSum");
                jobExecution.getExecutionContext().put("FINAL_RESULT", totalSum);
            }
        };
    }

}
