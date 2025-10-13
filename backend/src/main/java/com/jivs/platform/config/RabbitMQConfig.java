package com.jivs.platform.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ message queue configuration
 * Only loaded when RabbitMQ ConnectionFactory bean is available
 */
@Configuration
@EnableRabbit
@ConditionalOnBean(ConnectionFactory.class)
public class RabbitMQConfig {

    // Queue names
    public static final String EXTRACTION_QUEUE = "jivs.extraction.queue";
    public static final String MIGRATION_QUEUE = "jivs.migration.queue";
    public static final String DATA_QUALITY_QUEUE = "jivs.dataquality.queue";
    public static final String RETENTION_QUEUE = "jivs.retention.queue";
    public static final String COMPLIANCE_QUEUE = "jivs.compliance.queue";
    public static final String NOTIFICATION_QUEUE = "jivs.notification.queue";

    // Exchange names
    public static final String JIVS_EXCHANGE = "jivs.exchange";

    // Routing keys
    public static final String EXTRACTION_ROUTING_KEY = "extraction.#";
    public static final String MIGRATION_ROUTING_KEY = "migration.#";
    public static final String DATA_QUALITY_ROUTING_KEY = "dataquality.#";
    public static final String RETENTION_ROUTING_KEY = "retention.#";
    public static final String COMPLIANCE_ROUTING_KEY = "compliance.#";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.#";

    @Bean
    public TopicExchange jivExchange() {
        return new TopicExchange(JIVS_EXCHANGE, true, false);
    }

    @Bean
    public Queue extractionQueue() {
        return QueueBuilder.durable(EXTRACTION_QUEUE)
                .withArgument("x-max-priority", 10)
                .build();
    }

    @Bean
    public Queue migrationQueue() {
        return QueueBuilder.durable(MIGRATION_QUEUE)
                .withArgument("x-max-priority", 10)
                .build();
    }

    @Bean
    public Queue dataQualityQueue() {
        return QueueBuilder.durable(DATA_QUALITY_QUEUE)
                .build();
    }

    @Bean
    public Queue retentionQueue() {
        return QueueBuilder.durable(RETENTION_QUEUE)
                .build();
    }

    @Bean
    public Queue complianceQueue() {
        return QueueBuilder.durable(COMPLIANCE_QUEUE)
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .build();
    }

    @Bean
    public Binding extractionBinding(Queue extractionQueue, TopicExchange jivExchange) {
        return BindingBuilder.bind(extractionQueue).to(jivExchange).with(EXTRACTION_ROUTING_KEY);
    }

    @Bean
    public Binding migrationBinding(Queue migrationQueue, TopicExchange jivExchange) {
        return BindingBuilder.bind(migrationQueue).to(jivExchange).with(MIGRATION_ROUTING_KEY);
    }

    @Bean
    public Binding dataQualityBinding(Queue dataQualityQueue, TopicExchange jivExchange) {
        return BindingBuilder.bind(dataQualityQueue).to(jivExchange).with(DATA_QUALITY_ROUTING_KEY);
    }

    @Bean
    public Binding retentionBinding(Queue retentionQueue, TopicExchange jivExchange) {
        return BindingBuilder.bind(retentionQueue).to(jivExchange).with(RETENTION_ROUTING_KEY);
    }

    @Bean
    public Binding complianceBinding(Queue complianceQueue, TopicExchange jivExchange) {
        return BindingBuilder.bind(complianceQueue).to(jivExchange).with(COMPLIANCE_ROUTING_KEY);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange jivExchange) {
        return BindingBuilder.bind(notificationQueue).to(jivExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(10);
        factory.setConcurrentConsumers(5);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
}
