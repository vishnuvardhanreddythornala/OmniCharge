package com.omnicharge.recharge.messaging;

import com.omnicharge.common.event.RechargeCompletedEvent;
import com.omnicharge.common.event.saga.RechargeInitiatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RechargeEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private com.omnicharge.common.logging.LogEventPublisher logEventPublisher;

    @InjectMocks
    private RechargeEventProducer rechargeEventProducer;

    @Test
    void publishRechargeCompleted_Success() {
        RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                .rechargeId("OMNI-A1B2C3D4")
                .userId(1L)
                .amount(new BigDecimal("299.00"))
                .status("SUCCESS")
                .transactionId("TRX_777")
                .timestamp(LocalDateTime.now())
                .build();

        rechargeEventProducer.publishRechargeCompleted(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("omnicharge.exchange"), 
                eq("recharge.completed"), 
                eq(event)
        );
    }

    @Test
    void publishRechargeInitiated_Success() {
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("OMNI-P9O8I7U6")
                .userId(2L)
                .amount(new BigDecimal("199.00"))
                .paymentMethod("CREDIT_CARD")
                .timestamp(LocalDateTime.now())
                .build();

        rechargeEventProducer.publishRechargeInitiated(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("omnicharge.exchange"), 
                eq("saga.recharge.initiated"), 
                eq(event)
        );
    }

    @Test
    void publishes_ExceptionSwallowedSafely() {
        // Assume rabbit is completely down
        doThrow(new RuntimeException("RabbitMQ connection refused!"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        RechargeCompletedEvent event = RechargeCompletedEvent.builder().rechargeId("SWALLOW-ERR").build();

        // The producer class wraps publishes in a try-catch to log.error, so an exception should not propagate and crash the caller.
        rechargeEventProducer.publishRechargeCompleted(event);

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
