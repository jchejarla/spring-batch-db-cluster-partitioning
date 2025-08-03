package examples.io.github.jchejarla.springbatch.clustering.advancedjob;

import io.github.jchejarla.springbatch.clustering.api.ClusterAwarePartitioner;
import io.github.jchejarla.springbatch.clustering.api.PartitionStrategy;
import io.github.jchejarla.springbatch.clustering.autoconfigure.conditions.ConditionalOnClusterEnabled;
import io.github.jchejarla.springbatch.clustering.partition.ClusterAwarePartitionHandler;
import io.github.jchejarla.springbatch.clustering.partition.PartitionTransferableProp;
import io.github.jchejarla.springbatch.clustering.partition.PartitioningMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@EnableBatchProcessing
@ConditionalOnClusterEnabled
public class ETLJobConfig {

    @Autowired
    private FlatFileItemReader<Customer> customerItemReader;
    @Autowired
    private CustomerProcessor customerProcessor;
    @Autowired
    private StaxEventItemWriter<Customer> customerXmlWriter;

    @Autowired
    @Qualifier("etlJobPartitioner")
    private Partitioner partitioner;
    @Autowired
    private ClusterAwarePartitionHandler partitionHandler;

    @Bean("etlClusteredJob")
    public Job clusteredJob(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager, @Qualifier("multiStepAggregator") StepExecutionAggregator clusterAwareAggregator) {
        return new JobBuilder("etl-clustered-job", jobRepository)
                .incrementer(new RunIdIncrementer())
                .preventRestart()
                .start(multiNodeExecutionStep(jobRepository, platformTransactionManager, clusterAwareAggregator))
                .build();
    }

    public Step multiNodeExecutionStep(JobRepository jobRepository, PlatformTransactionManager txnManager, StepExecutionAggregator clusterAwareAggregator) {
        return new StepBuilder("etlStep.manager", jobRepository)
                .partitioner("multiNodeExecStep", partitioner)
                .partitionHandler(partitionHandler)
                .step(etlReaderWriterStep(jobRepository, txnManager))
                .aggregator(clusterAwareAggregator)
                .build();
    }

    @Bean
    public Step etlReaderWriterStep(JobRepository jobRepository, PlatformTransactionManager txnManager) {
        return new StepBuilder("etlReaderWriterStep", jobRepository).<Customer, Customer>chunk(100, txnManager)
                .reader(customerItemReader)
                .processor(customerProcessor)
                .writer(customerXmlWriter)
                .build();
    }

    @Bean("etlJobPartitioner")
    @StepScope
    public Partitioner etlJobPartitioner(@Value("#{jobParameters['rows']}") Integer rows) {
        return new ClusterAwarePartitioner() {

            @Override
            public List<ExecutionContext> createDistributedPartitions(int availableNodeCount) {
                int range = rows / availableNodeCount;
                List<ExecutionContext> partitions = new ArrayList<>(availableNodeCount);

                for (int i = 0; i < availableNodeCount; i++) {
                    ExecutionContext context = new ExecutionContext();
                    context.putInt("startRow", i * range + 1); // Skip header
                    context.putInt("endRow", (i + 1) * range);
                    context.putInt("partitionId", i);
                    partitions.add(context);
                }

                return partitions;
            }

            @Override
            public PartitionTransferableProp arePartitionsTransferableWhenNodeFailed() {
                return PartitionTransferableProp.YES;
            }

            @Override
            public PartitionStrategy buildPartitionStrategy() {
                return PartitionStrategy.builder().partitioningMode(PartitioningMode.ROUND_ROBIN).build();
            }
        };


    }
}
