package examples.io.github.jchejarla.springbatch.clustering.messaging;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;

import java.util.Arrays;

@Configuration
@EnableJms
@ConditionalOnProperty(value = "spring.batch.msg-channel.enabled", havingValue = "true")
public class ActiveMQConfig {

    @Value("${activemq.broker.url}")
    String activeMQBrokerURL;

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(activeMQBrokerURL);
        factory.setTrustedPackages(Arrays.asList(
                "java.lang",
                "java.util",
                "org.springframework.batch.integration.partition",
                "org.springframework.batch.core"
        ));

        return factory;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

    @Bean
    public JmsMessagingTemplate jmsMessagingTemplate(JmsTemplate jmsTemplate) {
        return new JmsMessagingTemplate(jmsTemplate);
    }

    @Bean
    public PollableChannel replies() {
        return new QueueChannel();
    }

    @Bean
    public MessageChannel requests() {
        return new DirectChannel();
    }
}
