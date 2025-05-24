package examples.dev.jchejarla.springbatch.clustering.simplejob;

import dev.jchejarla.springbatch.clustering.api.ClusterAwareAggregatorCallback;
import dev.jchejarla.springbatch.clustering.api.ClusterAwareAggregator;
import dev.jchejarla.springbatch.clustering.api.PartitionStrategy;
import dev.jchejarla.springbatch.clustering.partition.ClusterAwarePartitionHandler;
import dev.jchejarla.springbatch.clustering.api.ClusterAwarePartitioner;
import dev.jchejarla.springbatch.clustering.partition.PartitionTransferableProp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
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
public class SimpleJobConfig {

    @Autowired
    Partitioner partitioner;
    @Autowired
    ClusterAwarePartitionHandler partitionHandler;


    @Bean("clusteredJob")
    public Job clusteredJob(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager, @Qualifier("multiStepAggregator") StepExecutionAggregator clusterAwareAggregator) {
        return new JobBuilder("clustered-job", jobRepository)
                .incrementer(new RunIdIncrementer())
                .preventRestart()
                .start(singleNodeExecStep(jobRepository, platformTransactionManager))
                .next(multiNodeExecutionStep(jobRepository, platformTransactionManager, clusterAwareAggregator))
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
        ClusterAwareAggregatorCallback clusterAwareAggregatorCallback = new ClusterAwareAggregatorCallback() {

            @Override
            public void onSuccess(Collection<StepExecution> executions) {
                AtomicLong sum = new AtomicLong(0);
                executions.forEach(execution-> {
                    Long resultFromPartitionedTask = execution.getExecutionContext().get("result", Long.class);
                    if(Objects.nonNull(resultFromPartitionedTask)) {
                        sum.addAndGet(resultFromPartitionedTask);
                    }
                });
                log.info("Sum of number from 1...1000 is {} ", sum.longValue());
            }

            @Override
            public void onFailure(Collection<StepExecution> executions) {

            }
        };
        ClusterAwareAggregator clusterAwareAggregator = new ClusterAwareAggregator(clusterAwareAggregatorCallback);
        clusterAwareAggregator.setJobExplorer(jobExplorer);
        return clusterAwareAggregator;
    }

    @Bean
    public Step multiNodeWorkerStep(JobRepository jobRepository, PlatformTransactionManager txnManager) {
        return new StepBuilder("multiNodeWorkerStep", jobRepository)
                .tasklet(new MultiNodeExecutionTask(), txnManager)
                .build();
    }

    @Bean
    public Partitioner partitioner() {
        return new ClusterAwarePartitioner() {

            @Override
            public List<ExecutionContext> splitIntoChunksForDistribution(int availableNodeCount) {
                List<ExecutionContext> executionContexts = new ArrayList<>(availableNodeCount);
                int start = 0;
                for (int i=0; i<2; i++) {
                    ExecutionContext context = new ExecutionContext();
                    context.putString("partitionKey", "partition-" + i);
                    context.putInt("start", (i*start) + 1);
                    context.putInt("end", (i*start) + 500);
                    start = 500;
                    executionContexts.add(context);
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
}
