package examples.dev.jchejarla.springbatch.clustering.messaging;

import examples.dev.jchejarla.springbatch.clustering.common.Range;
import examples.dev.jchejarla.springbatch.clustering.common.RangeItemReader;
import examples.dev.jchejarla.springbatch.clustering.common.RangeSumProcessor;
import examples.dev.jchejarla.springbatch.clustering.common.RangeSumWriter;
import jakarta.jms.ConnectionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.launch.JobLaunchingMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@ConditionalOnProperty(prefix = "spring.batch.msg-channel", name = {"enabled", "worker-node"}, havingValue = "true")
public class WorkerConfig {
    @Autowired private JobRepository jobRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ConnectionFactory connectionFactory;
    @Autowired private JobExplorer jobExplorer;

    @Bean
    public Job workerJob() {
        return new JobBuilder("rangeSumJob", jobRepository)
                .start(workerStep())
                .build();
    }

  /*  @Bean
    public IntegrationFlow inboundFlow(StepExecutionRequestHandler stepExecutionRequestHandler) {
        return IntegrationFlow.from(Jms.messageDrivenChannelAdapter(connectionFactory)
                        .destination("partition.requests"))
                .handle(stepExecutionRequestHandler)
                .get();
    }*/

    @Bean
    public IntegrationFlow stepExecutionRequestListener(
            ConnectionFactory connectionFactory,
            JobLauncher jobLauncher,
            JobRegistry jobRegistry) {
        return IntegrationFlow.from(
                        Jms.messageDrivenChannelAdapter(connectionFactory)
                                .destination("partition.requests"))
                .handle(m->{
                    jobLaunchingMessageHandler(jobLauncher);
                })
                .get();
    }


    @Bean
    public JobLaunchingMessageHandler jobLaunchingMessageHandler(JobLauncher jobLauncher) {
        JobLaunchingMessageHandler handler = new JobLaunchingMessageHandler(jobLauncher);
        return handler;
    }


    @Bean
    public IntegrationFlow repliesToJmsFlow(JmsTemplate jmsTemplate, MessageChannel replies) {
        return IntegrationFlow.from(replies)
                .handle(Jms.outboundAdapter(jmsTemplate).destination("partition.replies"))
                .get();
    }

    /*@Bean
    public StepExecutionRequestHandler stepExecutionRequestHandler(JobExplorer jobExplorer,
                                                                   JobRepository jobRepository,
                                                                   StepLocator stepLocator) {
        StepExecutionRequestHandler handler = new StepExecutionRequestHandler();
        handler.setJobExplorer(jobExplorer);
        handler.setStepLocator(stepLocator);
        return handler;
    }*/

    @Bean
    public Step workerStep() {
        return new StepBuilder("workerStep", jobRepository)
                .<Range, Long>chunk(1, transactionManager)
                .reader(rangeItemReader(null, null))
                .processor(rangeSumProcessor())
                .writer(rangeSumWriter())
                .build();
    }

    @Bean
    @StepScope
    public RangeItemReader rangeItemReader(
            @Value("#{stepExecutionContext['start']}") Long start,
            @Value("#{stepExecutionContext['end']}") Long end) {
        return new RangeItemReader(new Range(start, end));
    }

    @Bean
    @StepScope
    public RangeSumProcessor rangeSumProcessor() {
        return new RangeSumProcessor();
    }

    @Bean
    @StepScope
    public RangeSumWriter rangeSumWriter() {
        return new RangeSumWriter();
    }

}
