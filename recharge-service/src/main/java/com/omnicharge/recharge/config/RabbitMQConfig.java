package com.omnicharge.recharge.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final String DLX_EXCHANGE = "omnicharge.dlx";
    private static final String APPROVED_DLQ_ROUTING_KEY = "saga.recharge.approved.dlq";
    private static final String REJECTED_DLQ_ROUTING_KEY = "saga.recharge.rejected.dlq";
    private static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    private static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("omnicharge.exchange");
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue paymentApprovedDlq() {
        return QueueBuilder.durable(APPROVED_DLQ_ROUTING_KEY).build();
    }

    @Bean
    public Queue paymentRejectedDlq() {
        return QueueBuilder.durable(REJECTED_DLQ_ROUTING_KEY).build();
    }

    @Bean
    public Binding paymentApprovedDlqBinding() {
        return BindingBuilder.bind(paymentApprovedDlq()).to(dlqExchange()).with(APPROVED_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding paymentRejectedDlqBinding() {
        return BindingBuilder.bind(paymentRejectedDlq()).to(dlqExchange()).with(REJECTED_DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue paymentApprovedQueue() {
        return QueueBuilder.durable("saga.recharge.approved")
                .withArgument(X_DEAD_LETTER_EXCHANGE, DLX_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, APPROVED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentRejectedQueue() {
        return QueueBuilder.durable("saga.recharge.rejected")
                .withArgument(X_DEAD_LETTER_EXCHANGE, DLX_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, REJECTED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding paymentApprovedBinding(Queue paymentApprovedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentApprovedQueue).to(exchange).with("saga.payment.approved");
    }

    @Bean
    public Binding paymentRejectedBinding(Queue paymentRejectedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentRejectedQueue).to(exchange).with("saga.payment.rejected");
    }

    @Bean
    public org.springframework.amqp.support.converter.DefaultClassMapper classMapper() {
        org.springframework.amqp.support.converter.DefaultClassMapper classMapper = new org.springframework.amqp.support.converter.DefaultClassMapper();
        java.util.Map<String, Class<?>> idClassMapping = new java.util.HashMap<>();
        
        idClassMapping.put("com.omnicharge.payment.common.event.saga.PaymentApprovedEvent", com.omnicharge.recharge.common.event.saga.PaymentApprovedEvent.class);
        idClassMapping.put("com.omnicharge.recharge.common.event.saga.PaymentApprovedEvent", com.omnicharge.recharge.common.event.saga.PaymentApprovedEvent.class);
        
        idClassMapping.put("com.omnicharge.payment.common.event.saga.PaymentRejectedEvent", com.omnicharge.recharge.common.event.saga.PaymentRejectedEvent.class);
        idClassMapping.put("com.omnicharge.recharge.common.event.saga.PaymentRejectedEvent", com.omnicharge.recharge.common.event.saga.PaymentRejectedEvent.class);
        
        classMapper.setIdClassMapping(idClassMapping);
        classMapper.setTrustedPackages("*");
        return classMapper;
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(classMapper());
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
