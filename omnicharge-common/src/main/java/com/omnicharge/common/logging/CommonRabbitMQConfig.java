package com.omnicharge.common.logging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared RabbitMQ infrastructure configuration for the logging pipeline.
 * 
 * This is auto-configured in EVERY service via Spring Boot AutoConfiguration.imports.
 * It ensures that the logging exchange, queue, and binding exist in RabbitMQ
 * BEFORE any service tries to publish log events. This prevents message loss
 * when services boot before the logging-service consumer is ready.
 * 
 * The queue is durable, so messages are persisted even if logging-service
 * hasn't started consuming yet.
 */
@Configuration
public class CommonRabbitMQConfig {

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter commonJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Declares the logging topic exchange.
     * Idempotent — safe to call from multiple services.
     */
    @Bean
    @ConditionalOnMissingBean(name = "loggingExchange")
    public TopicExchange loggingExchange() {
        return new TopicExchange(LoggingConstants.LOGGING_EXCHANGE);
    }

    /**
     * Declares the durable logging queue.
     * Messages are persisted until logging-service consumes them.
     */
    @Bean
    @ConditionalOnMissingBean(name = "loggingQueue")
    public Queue loggingQueue() {
        return QueueBuilder.durable(LoggingConstants.LOGGING_QUEUE).build();
    }

    /**
     * Binds the queue to the exchange with routing key "log.#"
     * so all log.{serviceName} messages are captured.
     */
    @Bean
    @ConditionalOnMissingBean(name = "loggingBinding")
    public Binding loggingBinding(Queue loggingQueue, TopicExchange loggingExchange) {
        return BindingBuilder
                .bind(loggingQueue)
                .to(loggingExchange)
                .with(LoggingConstants.LOGGING_ROUTING_KEY);
    }
}
