package com.omnicharge.payment.messaging;

import com.omnicharge.common.event.PaymentCompletedEvent;
import com.omnicharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private PaymentEventProducer paymentEventProducer;

    @Test
    void publishPaymentCompleted_Verified() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder().transactionId("1").build();
        paymentEventProducer.publishPaymentCompleted(event);
        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("payment.completed"), eq(event));
    }

    @Test
    void publishPaymentApproved_Verified() {
        PaymentApprovedEvent event = PaymentApprovedEvent.builder().rechargeId("A1").build();
        paymentEventProducer.publishPaymentApproved(event);
        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("saga.payment.approved"), eq(event));
    }

    @Test
    void publishPaymentRejected_Verified() {
        PaymentRejectedEvent event = PaymentRejectedEvent.builder().rechargeId("R1").build();
        paymentEventProducer.publishPaymentRejected(event);
        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("saga.payment.rejected"), eq(event));
    }

    @Test
    void exceptionCaughtAndSwallowedSafely() {
        doThrow(new RuntimeException("Rabbit timeout")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        PaymentCompletedEvent event = PaymentCompletedEvent.builder().transactionId("Z1").build();
        paymentEventProducer.publishPaymentCompleted(event); // Should not throw
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
