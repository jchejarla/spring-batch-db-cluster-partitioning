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
package examples.io.github.jchejarla.springbatch.clustering.messaging;

import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.partition.MessageChannelPartitionHandler;
import org.springframework.batch.infrastructure.item.ExecutionContext;
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