package com.omnicharge.payment.consumer;

import com.omnicharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.common.event.saga.RechargeInitiatedEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.service.IPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSagaConsumerTest {

    @Mock
    private IPaymentService paymentService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private PaymentSagaConsumer paymentSagaConsumer;

    @Test
    void consumeRechargeInitiatedEvent_Success_PublishesApproved() {
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("OMNI-S1")
                .userId(1L)
                .amount(new BigDecimal("99.00"))
                .build();

        PaymentResponse mockResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId("TXN-1")
                .amount(new BigDecimal("99.00"))
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

        paymentSagaConsumer.consumeRechargeInitiatedEvent(event);

        ArgumentCaptor<PaymentApprovedEvent> captor = ArgumentCaptor.forClass(PaymentApprovedEvent.class);
        verify(paymentEventProducer, times(1)).publishPaymentApproved(captor.capture());
        assertEquals("OMNI-S1", captor.getValue().getRechargeId());
    }

    @Test
    void consumeRechargeInitiatedEvent_Pending_SwallowsAndAwaitsWebhook() {
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("OMNI-S2")
                .userId(2L)
                .amount(new BigDecimal("199.00"))
                .build();

        PaymentResponse mockResponse = PaymentResponse.builder().status("PENDING").build();
        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

        paymentSagaConsumer.consumeRechargeInitiatedEvent(event);

        verify(paymentEventProducer, never()).publishPaymentApproved(any());
        verify(paymentEventProducer, never()).publishPaymentRejected(any());
    }

    @Test
    void consumeRechargeInitiatedEvent_Failed_PublishesRejected() {
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("OMNI-S3")
                .userId(3L)
                .amount(new BigDecimal("299.00"))
                .build();

        PaymentResponse mockResponse = PaymentResponse.builder().status("FAILED").build();
        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

        paymentSagaConsumer.consumeRechargeInitiatedEvent(event);

        ArgumentCaptor<PaymentRejectedEvent> captor = ArgumentCaptor.forClass(PaymentRejectedEvent.class);
        verify(paymentEventProducer, times(1)).publishPaymentRejected(captor.capture());
        assertEquals("OMNI-S3", captor.getValue().getRechargeId());
        assertEquals("Payment creation failed via Razorpay", captor.getValue().getFailureReason());
    }

    @Test
    void consumeRechargeInitiatedEvent_Exception_PublishesRejected() {
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("OMNI-S4")
                .userId(4L)
                .amount(new BigDecimal("399.00"))
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenThrow(new RuntimeException("DB Outage"));

        paymentSagaConsumer.consumeRechargeInitiatedEvent(event);

        ArgumentCaptor<PaymentRejectedEvent> captor = ArgumentCaptor.forClass(PaymentRejectedEvent.class);
        verify(paymentEventProducer, times(1)).publishPaymentRejected(captor.capture());
        assertEquals("OMNI-S4", captor.getValue().getRechargeId());
        assertEquals("DB Outage", captor.getValue().getFailureReason());
    }
}
