package com.omnicharge.payment.config;

import org.springframework.amqp.core.TopicExchange;
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
    public org.springframework.amqp.core.Queue paymentProcessQueue() {
        return new org.springframework.amqp.core.Queue("saga.payment.process");
    }

    @Bean
    public org.springframework.amqp.core.Binding paymentProcessBinding(org.springframework.amqp.core.Queue paymentProcessQueue, TopicExchange exchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(paymentProcessQueue).to(exchange).with("saga.recharge.initiated");
    }

    @Bean
    public org.springframework.amqp.support.converter.DefaultClassMapper classMapper() {
        org.springframework.amqp.support.converter.DefaultClassMapper classMapper = new org.springframework.amqp.support.converter.DefaultClassMapper();
        java.util.Map<String, Class<?>> idClassMapping = new java.util.HashMap<>();
        
        idClassMapping.put("com.omnicharge.recharge.common.event.saga.RechargeInitiatedEvent", com.omnicharge.payment.common.event.saga.RechargeInitiatedEvent.class);
        idClassMapping.put("com.omnicharge.payment.common.event.saga.RechargeInitiatedEvent", com.omnicharge.payment.common.event.saga.RechargeInitiatedEvent.class);
        
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
