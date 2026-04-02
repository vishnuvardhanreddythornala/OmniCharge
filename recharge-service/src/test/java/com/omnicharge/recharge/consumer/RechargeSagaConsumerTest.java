package com.omnicharge.recharge.consumer;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.common.event.RechargeCompletedEvent;
import com.omnicharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.UserProfileResponse;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.messaging.RechargeEventProducer;
import com.omnicharge.recharge.repository.RechargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private com.omnicharge.common.logging.LogEventPublisher logEventPublisher;

    @InjectMocks
    private RechargeSagaConsumer rechargeSagaConsumer;

    private Recharge recharge;
    private UserProfileResponse userProfile;

    @BeforeEach
    void setUp() {
        recharge = new Recharge();
        recharge.setId(100L);
        recharge.setRechargeId("OMNI-SAGA123");
        recharge.setUserId(1L);
        recharge.setStatus(RechargeStatus.PROCESSING);
        recharge.setAmount(new java.math.BigDecimal("299.00")); // Add amount
        
        userProfile = new UserProfileResponse();
        userProfile.setEmail("user@example.com");
    }

    @Test
    void consumePaymentApprovedEvent_Success_TriggersNotification() {
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .rechargeId("OMNI-SAGA123")
                .transactionId("PAY_999")
                .build();

        when(rechargeRepository.findByRechargeId("OMNI-SAGA123")).thenReturn(Optional.of(recharge));
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.success("OK", userProfile));

        rechargeSagaConsumer.consumePaymentApprovedEvent(event);

        // Verify entity transitioned directly to SUCCESS
        assertEquals(RechargeStatus.SUCCESS, recharge.getStatus());
        assertEquals("PAY_999", recharge.getTransactionId());
        verify(rechargeRepository, times(1)).save(recharge);

        // Verify cascading event publishing for Notification System mapping
        ArgumentCaptor<RechargeCompletedEvent> captor = ArgumentCaptor.forClass(RechargeCompletedEvent.class);
        verify(rechargeEventProducer, times(1)).publishRechargeCompleted(captor.capture());
        
        RechargeCompletedEvent completedEvent = captor.getValue();
        assertEquals("SUCCESS", completedEvent.getStatus());
        assertEquals("user@example.com", completedEvent.getUserEmail());
    }

    @Test
    void consumePaymentRejectedEvent_Fails_BypassesNotification() {
        PaymentRejectedEvent event = PaymentRejectedEvent.builder()
                .rechargeId("OMNI-SAGA123")
                .failureReason("Insufficient Funds in Wallet")
                .build();

        when(rechargeRepository.findByRechargeId("OMNI-SAGA123")).thenReturn(Optional.of(recharge));

        rechargeSagaConsumer.consumePaymentRejectedEvent(event);

        // Verify entity reduction to FAILED
        assertEquals(RechargeStatus.FAILED, recharge.getStatus());
        assertEquals("Insufficient Funds in Wallet", recharge.getFailureReason());
        verify(rechargeRepository, times(1)).save(recharge);

        // Notifications or successful callbacks are explicitly bypassed on Saga Rejected.
        verify(rechargeEventProducer, never()).publishRechargeCompleted(any());
        verify(userServiceClient, never()).getUserById(anyLong());
    }

    @Test
    void consumePaymentApprovedEvent_RechargeNotFound_IgnoresEventSafely() {
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .rechargeId("UNKNOWN-ID")
                .build();

        when(rechargeRepository.findByRechargeId("UNKNOWN-ID")).thenReturn(Optional.empty());

        // Exception shouldn't bubble up; just logs error due to ifPresentOrElse
        assertDoesNotThrow(() -> rechargeSagaConsumer.consumePaymentApprovedEvent(event));
        verify(rechargeRepository, never()).save(any());
    }
}
