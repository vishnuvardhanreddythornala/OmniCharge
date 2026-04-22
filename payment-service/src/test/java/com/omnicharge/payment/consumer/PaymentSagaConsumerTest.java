package com.omnicharge.payment.consumer;

import com.omnicharge.payment.common.event.saga.RechargeInitiatedEvent;
import com.omnicharge.payment.common.logging.LogEvent;
import com.omnicharge.payment.common.logging.LogEventPublisher;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.service.IPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

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
    void consumeRechargeInitiatedEvent_SuccessLogAndIgnoreCreation() {
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("REC-123")
                .userId(1L)
                .amount(new BigDecimal("199.00"))
                .paymentMethod("UPI")
                .build();

        paymentSagaConsumer.consumeRechargeInitiatedEvent(event);

        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
        verify(paymentService, never()).processPayment(any());
    }
}
