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

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("omnicharge.exchange");
    }

    @Bean
    public Queue paymentApprovedQueue() {
        return new Queue("saga.recharge.approved");
    }

    @Bean
    public Queue paymentRejectedQueue() {
        return new Queue("saga.recharge.rejected");
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
