package examples.dev.jchejarla.springbatch.clustering.simplejob;

import dev.jchejarla.springbatch.clustering.api.ClusterAwareAggregatorCallback;
import dev.jchejarla.springbatch.clustering.api.ClusterAwareAggregator;
import dev.jchejarla.springbatch.clustering.api.PartitionStrategy;
import dev.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import dev.jchejarla.springbatch.clustering.partition.ClusterAwarePartitionHandler;
import dev.jchejarla.springbatch.clustering.api.ClusterAwarePartitioner;
import dev.jchejarla.springbatch.clustering.partition.PartitionTransferableProp;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
@EnableBatchProcessing
@ConditionalOnClusterEnabled
public class SimpleJobConfig {

    @Autowired
    Partitioner partitioner;
    @Autowired
    ClusterAwarePartitionHandler partitionHandler;
    @Getter
    SumAggregatorCallback sumAggregatorCallback;

    @Bean("clusteredJob")
    public Job clusteredJob(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager, @Qualifier("multiStepAggregator") StepExecutionAggregator clusterAwareAggregator) {
        return new JobBuilder("clustered-job", jobRepository)
                .incrementer(new RunIdIncrementer())
                .preventRestart()
                //.start(singleNodeExecStep(jobRepository, platformTransactionManager))
                .start(multiNodeExecutionStep(jobRepository, platformTransactionManager, clusterAwareAggregator))
                .build();
    }

    @Bean
    public Step singleNodeExecStep(JobRepository jobRepository, PlatformTransactionManager txnManager) {
        return new StepBuilder("singleNodeExecStep", jobRepository)
                .tasklet(new SingleNodeExecutionTask(), txnManager)
                .build();
    }

    public Step multiNodeExecutionStep(JobRepository jobRepository, PlatformTransactionManager txnManager, StepExecutionAggregator clusterAwareAggregator) {
        return new StepBuilder("multiNodeExecStep.manager", jobRepository)
                .partitioner("multiNodeExecStep", partitioner)
                .partitionHandler(partitionHandler)
                .step(multiNodeWorkerStep(jobRepository, txnManager))
                .aggregator(clusterAwareAggregator)
                .build();
    }

    @Bean("multiStepAggregator")
    public StepExecutionAggregator aggregator(JobExplorer jobExplorer) {
        sumAggregatorCallback = new SumAggregatorCallback();
        ClusterAwareAggregator clusterAwareAggregator = new ClusterAwareAggregator(sumAggregatorCallback);
        clusterAwareAggregator.setJobExplorer(jobExplorer);
        return clusterAwareAggregator;
    }

    @Bean
    public Step multiNodeWorkerStep(JobRepository jobRepository, PlatformTransactionManager txnManager) {
        return new StepBuilder("multiNodeWorkerStep", jobRepository)
                .tasklet(new LargeSumExecutionTask(), txnManager)
                .build();
    }

    @Bean
    @StepScope
    public Partitioner partitioner(@Value("#{jobParameters['taskSize']}") Long tasksSize, @Value("#{jobParameters['from']}") Long from, @Value("#{jobParameters['to']}") Long to) {
        return new ClusterAwarePartitioner() {

            @Override
            public List<ExecutionContext> splitIntoChunksForDistribution(int availableNodeCount) {
                List<ExecutionContext> executionContexts = new ArrayList<>(availableNodeCount);

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
                    executionContexts.add(context);
                    currentStart = currentEnd +1;
                }
                return executionContexts;
            }

            @Override
            public PartitionTransferableProp arePartitionsTransferableWhenNodeFailed() {
                return PartitionTransferableProp.YES;
            }

            @Override
            public PartitionStrategy buildPartitionStrategy() {
                return null;
            }
        };


    }

    @Getter
    public static class SumAggregatorCallback implements ClusterAwareAggregatorCallback {

        public AtomicLong sum = new AtomicLong(0);

        @Override
        public void onSuccess(Collection<StepExecution> executions) {
            executions.forEach(execution-> {
                Long resultFromPartitionedTask = execution.getExecutionContext().get("result", Long.class);
                if(Objects.nonNull(resultFromPartitionedTask)) {
                    sum.addAndGet(resultFromPartitionedTask);
                }
            });
            log.info("Sum of input range is {} ", sum.longValue());
        }

        @Override
        public void onFailure(Collection<StepExecution> executions) {
        }
    }
}
