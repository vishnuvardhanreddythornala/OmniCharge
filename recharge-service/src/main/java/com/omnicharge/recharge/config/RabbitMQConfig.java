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
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
