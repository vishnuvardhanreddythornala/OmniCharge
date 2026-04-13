package com.omnicharge.recharge.messaging;

import com.omnicharge.recharge.common.event.RechargeCompletedEvent;
import com.omnicharge.recharge.common.event.saga.RechargeInitiatedEvent;
import com.omnicharge.recharge.common.logging.LogEvent;
import com.omnicharge.recharge.common.logging.LogEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RechargeEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private RechargeEventProducer rechargeEventProducer;

    @Test
    void publishRechargeCompleted_Success() {
        RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                .rechargeId("REC-123")
                .userId(1L)
                .status("SUCCESS")
                .build();

        rechargeEventProducer.publishRechargeCompleted(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("recharge.completed"), eq(event));
        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getEventType().equals("SAGA_EVENT_PUBLISHED")));
    }

    @Test
    void publishRechargeCompleted_Failure() {
        RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                .rechargeId("REC-123")
                .build();

        doThrow(new RuntimeException("Rabbit MQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        rechargeEventProducer.publishRechargeCompleted(event);

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), eq(event));
        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getEventType().equals("SAGA_EVENT_PUBLISH_FAILED")));
    }

    @Test
    void publishRechargeInitiated_Success() {
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("REC-123")
                .amount(new BigDecimal("199.00"))
                .userId(1L)
                .paymentMethod("UPI")
                .build();

        rechargeEventProducer.publishRechargeInitiated(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("saga.recharge.initiated"), eq(event));
        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getEventType().equals("SAGA_EVENT_PUBLISHED")));
    }

    @Test
    void publishRechargeInitiated_Failure() {
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("REC-123")
                .build();

        doThrow(new RuntimeException("Connection closed")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        rechargeEventProducer.publishRechargeInitiated(event);

        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getEventType().equals("SAGA_EVENT_PUBLISH_FAILED")));
    }
}
