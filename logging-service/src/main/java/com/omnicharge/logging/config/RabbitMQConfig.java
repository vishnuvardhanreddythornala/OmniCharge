package com.omnicharge.logging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.omnicharge.logging.common.logging.LoggingConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the logging-service.
 * 
 * Declares a SEPARATE exchange (omnicharge.logging.exchange) that is
 * completely isolated from the business Saga exchange (omnicharge.exchange).
 * This ensures zero interference with existing business flows.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange loggingExchange() {
        return new TopicExchange(LoggingConstants.LOGGING_EXCHANGE);
    }

    @Bean
    public Queue loggingQueue() {
        return QueueBuilder.durable(LoggingConstants.LOGGING_QUEUE).build();
    }

    @Bean
    public Binding loggingBinding(Queue loggingQueue, TopicExchange loggingExchange) {
        return BindingBuilder
                .bind(loggingQueue)
                .to(loggingExchange)
                .with(LoggingConstants.LOGGING_ROUTING_KEY);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        
        // Instruct the type mapper to always return the local LogEvent type.
        // This completely bypasses the __TypeId__ header from the publisher,
        // preventing ClassNotFoundExceptions when packages don't match.
        org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper typeMapper = 
            new org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper() {
                @Override
                public com.fasterxml.jackson.databind.JavaType toJavaType(org.springframework.amqp.core.MessageProperties properties) {
                    return com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance()
                            .constructType(com.omnicharge.logging.common.logging.LogEvent.class);
                }
            };
        converter.setJavaTypeMapper(typeMapper);
        
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory factory = new org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
