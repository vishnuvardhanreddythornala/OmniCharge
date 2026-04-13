package com.omnicharge.recharge.consumer;

import com.omnicharge.recharge.common.dto.ApiResponse;
import com.omnicharge.recharge.common.event.RechargeCompletedEvent;
import com.omnicharge.recharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.recharge.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.recharge.common.logging.LogEvent;
import com.omnicharge.recharge.common.logging.LogEventPublisher;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.UserProfileResponse;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.messaging.RechargeEventProducer;
import com.omnicharge.recharge.repository.RechargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RechargeSagaConsumerTest {

    @Mock
    private RechargeRepository rechargeRepository;

    @Mock
    private RechargeEventProducer rechargeEventProducer;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private RechargeSagaConsumer rechargeSagaConsumer;

    private Recharge processingRecharge;

    @BeforeEach
    void setUp() {
        processingRecharge = new Recharge();
        processingRecharge.setId(10L);
        processingRecharge.setRechargeId("OMNI-REC123");
        processingRecharge.setUserId(1L);
        processingRecharge.setAmount(new BigDecimal("199.00"));
        processingRecharge.setStatus(RechargeStatus.PROCESSING);
    }

    @Test
    void consumePaymentApprovedEvent_Success() {
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .rechargeId("OMNI-REC123")
                .transactionId("TXN-999")
                .amount(new BigDecimal("199.00"))
                .build();

        UserProfileResponse userProfile = new UserProfileResponse();
        userProfile.setEmail("test@ex.com");
        userProfile.setMobileNumber("9876543210");
        
        when(rechargeRepository.findByRechargeId("OMNI-REC123")).thenReturn(Optional.of(processingRecharge));
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.success(userProfile));

        rechargeSagaConsumer.consumePaymentApprovedEvent(event);

        assertEquals(RechargeStatus.SUCCESS, processingRecharge.getStatus());
        assertEquals("TXN-999", processingRecharge.getTransactionId());
        verify(rechargeRepository, times(1)).save(processingRecharge);
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
        verify(rechargeEventProducer, times(1)).publishRechargeCompleted(any(RechargeCompletedEvent.class));
    }

    @Test
    void consumePaymentApprovedEvent_RechargeNotFound() {
        PaymentApprovedEvent event = PaymentApprovedEvent.builder().rechargeId("MISSING").build();
        when(rechargeRepository.findByRechargeId("MISSING")).thenReturn(Optional.empty());

        rechargeSagaConsumer.consumePaymentApprovedEvent(event);

        verify(rechargeRepository, never()).save(any());
        verify(rechargeEventProducer, never()).publishRechargeCompleted(any());
    }

    @Test
    void consumePaymentRejectedEvent_Success() {
        PaymentRejectedEvent event = PaymentRejectedEvent.builder()
                .rechargeId("OMNI-REC123")
                .failureReason("User Cancelled")
                .build();

        when(rechargeRepository.findByRechargeId("OMNI-REC123")).thenReturn(Optional.of(processingRecharge));

        rechargeSagaConsumer.consumePaymentRejectedEvent(event);

        assertEquals(RechargeStatus.FAILED, processingRecharge.getStatus());
        assertEquals("User Cancelled", processingRecharge.getFailureReason());
        verify(rechargeRepository, times(1)).save(processingRecharge);
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void consumePaymentRejectedEvent_DuplicateSuccessIgnored() {
        processingRecharge.setStatus(RechargeStatus.SUCCESS);
        PaymentRejectedEvent event = PaymentRejectedEvent.builder()
                .rechargeId("OMNI-REC123")
                .failureReason("Late Reject Signal")
                .build();

        when(rechargeRepository.findByRechargeId("OMNI-REC123")).thenReturn(Optional.of(processingRecharge));

        rechargeSagaConsumer.consumePaymentRejectedEvent(event);

        assertEquals(RechargeStatus.SUCCESS, processingRecharge.getStatus());
        verify(rechargeRepository, never()).save(any());
        verify(logEventPublisher, never()).publish(any(LogEvent.class));
    }
}
