package com.omnicharge.user.common.logging;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonRabbitMQConfig {

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter commonJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
