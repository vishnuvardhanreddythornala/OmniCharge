package com.omnicharge.operator.messaging;

import com.omnicharge.operator.config.RabbitMQConfig;
import com.omnicharge.operator.event.PlanUpdatedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class OperatorEventPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private OperatorEventPublisher operatorEventPublisher;

    @Test
    void publishPlanUpdatedEvent_NoTransaction() {
        // When no transaction sync is active, sends immediately
        operatorEventPublisher.publishPlanUpdatedEvent(1L);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq("plan.updated"), any(PlanUpdatedMessage.class));
    }

    @Test
    void publishPlanUpdatedEvent_SendFailure() {
        doThrow(new RuntimeException("MQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(PlanUpdatedMessage.class));

        operatorEventPublisher.publishPlanUpdatedEvent(1L);

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(PlanUpdatedMessage.class));
    }
}
