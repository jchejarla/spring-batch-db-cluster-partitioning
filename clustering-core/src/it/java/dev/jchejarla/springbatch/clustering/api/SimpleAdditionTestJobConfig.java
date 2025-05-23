package dev.jchejarla.springbatch.clustering.api;

import dev.jchejarla.springbatch.clustering.partition.ClusterAwarePartitionHandler;
import dev.jchejarla.springbatch.clustering.partition.PartitionTransferableProp;
import lombok.Getter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@TestConfiguration
public class SimpleAdditionTestJobConfig {

    @Autowired
    private ClusterAwarePartitionHandler partitionHandler;

    @Getter
    SimpleSumAggregatorCallback simpleSumAggregatorCallback;

    @Bean("clusteredJob")
    public Job clusteredJob(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager,
                            StepExecutionAggregator stepExecutionAggregator, @Qualifier("multiNodeTaskPartitioner") Partitioner partitioner) {
        return new JobBuilder("clustered-job", jobRepository)
                .incrementer(new RunIdIncrementer())
                .preventRestart()
                .start(singleNodeStep(jobRepository, platformTransactionManager))
                .next(multiNodeExecutionStep(jobRepository, platformTransactionManager, partitioner, stepExecutionAggregator))
                .build();
    }

    @Bean
    public Step singleNodeStep(JobRepository jobRepository, PlatformTransactionManager txnManager) {
        return new StepBuilder("singleNodeExecStep", jobRepository)
                .tasklet(new SingleNodeTask(), txnManager)
                .build();
    }

    @Bean
    public Step multiNodeExecutionStep(JobRepository jobRepository, PlatformTransactionManager txnManager, Partitioner partitioner, StepExecutionAggregator stepExecutionAggregator) {
        return new StepBuilder("multiNodeExecStep.manager", jobRepository)
                .partitioner("multiNodeExecStep", partitioner)
                .partitionHandler(partitionHandler)
                .step(multiNodeWorkerStep(jobRepository, txnManager))
                .aggregator(stepExecutionAggregator)
                .build();
    }

    @Bean
    public Step multiNodeWorkerStep(JobRepository jobRepository, PlatformTransactionManager txnManager) {
        return new StepBuilder("multiNodeWorkerStep", jobRepository)
                .tasklet(new MultiNodeTask(), txnManager)
                .build();
    }

    @Bean
    public StepExecutionAggregator aggregator(JobExplorer jobExplorer) {
        simpleSumAggregatorCallback = new SimpleSumAggregatorCallback();
        ClusterAwareAggregator clusterAwareAggregator = new ClusterAwareAggregator(simpleSumAggregatorCallback);
        clusterAwareAggregator.setJobExplorer(jobExplorer);
        return clusterAwareAggregator;
    }

    @Bean("multiNodeTaskPartitioner")
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


    protected static class SimpleSumAggregatorCallback implements ClusterAwareAggregatorCallback {

        final AtomicLong sum = new AtomicLong(0);

        @Override
        public void onSuccess(Collection<StepExecution> executions) {
            executions.forEach(execution-> {
                Long resultFromPartitionedTask = execution.getExecutionContext().get("result", Long.class);
                if(Objects.nonNull(resultFromPartitionedTask)) {
                    sum.addAndGet(resultFromPartitionedTask);
                }
            });
        }

        @Override
        public void onFailure(Collection<StepExecution> executions) {}

        public AtomicLong getSum() {
            return sum;
        }
    }

}
