package com.omnicharge.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.support.converter.DefaultClassMapper;
import com.omnicharge.notification.dto.OtpEvent;

import java.util.Map;
import java.util.HashMap;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("omnicharge.exchange");
    }

    @Bean
    public Queue rechargeQueue() {
        return new Queue("notification.recharge.queue", true);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue("notification.payment.queue", true);
    }

    @Bean
    public Binding rechargeBinding(Queue rechargeQueue, TopicExchange exchange) {
        return BindingBuilder.bind(rechargeQueue).to(exchange).with("recharge.completed");
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentQueue).to(exchange).with("payment.completed");
    }

    @Bean
    public Queue planExpiryQueue() {
        return new Queue("notification.plan.expiry.queue", true);
    }

    @Bean
    public Binding planExpiryBinding(Queue planExpiryQueue, TopicExchange exchange) {
        return BindingBuilder.bind(planExpiryQueue).to(exchange).with("plan.expiry");
    }

    @Bean
    public Queue otpQueue() {
        return new Queue("notification.otp.queue", true);
    }

    @Bean
    public Binding otpBinding(Queue otpQueue, TopicExchange exchange) {
        return BindingBuilder.bind(otpQueue).to(exchange).with("mobile.otp.send");
    }

    @Bean
    public Binding emailOtpBinding(Queue otpQueue, TopicExchange exchange) {
        return BindingBuilder.bind(otpQueue).to(exchange).with("email.otp.send");
    }

    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        
        // Map OtpEvent
        idClassMapping.put("com.omnicharge.user.dto.OtpEvent", OtpEvent.class);
        idClassMapping.put("com.omnicharge.notification.dto.OtpEvent", OtpEvent.class);
        
        // Map PaymentCompletedEvent
        idClassMapping.put("com.omnicharge.payment.common.event.PaymentCompletedEvent", com.omnicharge.notification.common.event.PaymentCompletedEvent.class);
        idClassMapping.put("com.omnicharge.notification.common.event.PaymentCompletedEvent", com.omnicharge.notification.common.event.PaymentCompletedEvent.class);
        
        // Map RechargeCompletedEvent
        idClassMapping.put("com.omnicharge.recharge.common.event.RechargeCompletedEvent", com.omnicharge.notification.common.event.RechargeCompletedEvent.class);
        idClassMapping.put("com.omnicharge.notification.common.event.RechargeCompletedEvent", com.omnicharge.notification.common.event.RechargeCompletedEvent.class);
        
        // Map PlanExpiryEvent
        idClassMapping.put("com.omnicharge.recharge.common.event.PlanExpiryEvent", com.omnicharge.notification.common.event.PlanExpiryEvent.class);
        idClassMapping.put("com.omnicharge.notification.common.event.PlanExpiryEvent", com.omnicharge.notification.common.event.PlanExpiryEvent.class);
        
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
