package com.omnicharge.payment.messaging;

import com.omnicharge.payment.common.event.PaymentCompletedEvent;
import com.omnicharge.payment.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.payment.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.payment.common.logging.LogEvent;
import com.omnicharge.payment.common.logging.LogEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private PaymentEventProducer paymentEventProducer;

    @Test
    void publishPaymentCompleted_Success() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("TXN-123")
                .rechargeId("OMNI-456")
                .userId(1L)
                .status("SUCCESS")
                .build();

        paymentEventProducer.publishPaymentCompleted(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("payment.completed"), eq(event));
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void publishPaymentCompleted_Failure() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder().transactionId("TXN-123").build();
        doThrow(new RuntimeException("RabbitMQ connection down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        // Ensure no exception is thrown out of the producer
        paymentEventProducer.publishPaymentCompleted(event);

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), eq(event));
        verify(logEventPublisher, never()).publish(any(LogEvent.class));
    }

    @Test
    void publishPaymentApproved_Success() {
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .rechargeId("OMNI-456")
                .transactionId("TXN-123")
                .amount(BigDecimal.TEN)
                .status("SUCCESS")
                .build();

        paymentEventProducer.publishPaymentApproved(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("saga.payment.approved"), eq(event));
        ArgumentCaptor<LogEvent> logCaptor = ArgumentCaptor.forClass(LogEvent.class);
        verify(logEventPublisher, times(1)).publish(logCaptor.capture());
        assertEquals("SAGA_EVENT_PUBLISHED", logCaptor.getValue().getEventType());
    }

    @Test
    void publishPaymentRejected_Success() {
        PaymentRejectedEvent event = PaymentRejectedEvent.builder()
                .rechargeId("OMNI-456")
                .failureReason("Insufficient Funds")
                .build();

        paymentEventProducer.publishPaymentRejected(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("saga.payment.rejected"), eq(event));
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }
}
