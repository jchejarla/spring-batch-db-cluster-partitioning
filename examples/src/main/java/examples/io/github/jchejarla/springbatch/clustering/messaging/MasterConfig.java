package examples.io.github.jchejarla.springbatch.clustering.messaging;

import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.partition.MessageChannelPartitionHandler;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableBatchProcessing
@ConditionalOnProperty(prefix = "spring.batch.msg-channel", name = {"enabled", "master-node"}, havingValue = "true")
public class MasterConfig {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private JobExplorer jobExplorer;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private Partitioner partitioner;

    @Bean
    public Job rangeSumJob() {
        return new JobBuilder("rangeSumJob", jobRepository)
                .start(masterStep())
                .listener(jobListener())
                .build();
    }

    @Bean
    public Step masterStep() {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner("workerStep", partitioner)
                .partitionHandler(partitionHandler(null, null))
                .build();
    }

    @Bean
    public IntegrationFlow requestsToJmsFlow(JmsTemplate jmsTemplate, MessageChannel requests) {
        return IntegrationFlow.from(requests)
                .handle(Jms.outboundAdapter(jmsTemplate).destination("partition.requests"))
                .get();
    }

    @Bean
    public IntegrationFlow repliesFromJmsFlow(ConnectionFactory connectionFactory, PollableChannel replies) {
        return IntegrationFlow.from(Jms.messageDrivenChannelAdapter(connectionFactory)
                        .destination("partition.replies"))
                .channel(replies)
                .get();
    }

    @Bean
    public PartitionHandler partitionHandler(PollableChannel replies, MessageChannel requests) {
        MessageChannelPartitionHandler partitionHandler = new MessageChannelPartitionHandler();
        partitionHandler.setStepName("workerStep");
        partitionHandler.setGridSize(2);
        partitionHandler.setReplyChannel(replies);
        MessagingTemplate template = new MessagingTemplate();
        template.setDefaultChannel(requests);
        template.setReceiveTimeout(100000);
        partitionHandler.setMessagingOperations(template);
        return partitionHandler;
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
    public JobExecutionListener jobListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {}

            @Override
            public void afterJob(JobExecution jobExecution) {
                long total = jobExecution.getStepExecutions().stream()
                        .mapToLong(step -> step.getExecutionContext().getLong("partitionSum", 0))
                        .sum();
                log.info("TOTAL SUM: {}" , total);
                jobExecution.getExecutionContext().putLong("totalSum", total);
            }
        };
    }


}