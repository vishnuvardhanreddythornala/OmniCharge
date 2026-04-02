package com.omnicharge.recharge.messaging;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.consumer.RechargeSagaConsumer;
import com.omnicharge.recharge.dto.UserProfileResponse;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.repository.RechargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Property-based test for RabbitMQ SAGA event logging in recharge-service.
 * 
 * Validates Property 4: RabbitMQ SAGA Event Logging
 * "For any SAGA event (recharge initiated, payment approved, payment rejected),
 * the system should log the event publication with event type, rechargeId, routing key,
 * and relevant payload summary."
 * 
 * This test verifies that SAGA orchestration events (payment approved/rejected)
 * are logged with appropriate context including state transitions.
 * 
 * Feature: production-grade-centralized-logging, Property 4: RabbitMQ SAGA Event Logging
 * Validates: Requirements 3.1, 3.2
 */
@ExtendWith(MockitoExtension.class)
@Tag("property-test")
public class RechargeSagaEventLoggingPropertyTest {

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

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
    }

    /**
     * Property: For any PaymentApprovedEvent, the system logs SAGA_PAYMENT_APPROVED event
     * with rechargeId, userId, transactionId, previousStatus, currentStatus, and amount.
     */
    @Test
    void property_paymentApproved_logsSagaEventWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random SAGA event data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));

            Recharge recharge = new Recharge();
            recharge.setId(random.nextLong(1, 100000));
            recharge.setRechargeId(rechargeId);
            recharge.setUserId(userId);
            recharge.setStatus(RechargeStatus.PROCESSING);
            recharge.setAmount(amount);
            recharge.setMobileNumber("9" + String.format("%09d", random.nextInt(1000000000)));
            recharge.setOperatorName("Operator-" + random.nextInt(10));
            recharge.setPlanName("Plan-" + random.nextInt(100));

            when(rechargeRepository.findByRechargeId(rechargeId)).thenReturn(Optional.of(recharge));
            when(rechargeRepository.save(any(Recharge.class))).thenReturn(recharge);

            UserProfileResponse userProfile = UserProfileResponse.builder()
                    .id(userId)
                    .email("user" + userId + "@example.com")
                    .mobileNumber(recharge.getMobileNumber())
                    .build();
            ApiResponse<UserProfileResponse> userApiResponse = new ApiResponse<>(true, "Success", userProfile);
            when(userServiceClient.getUserById(userId)).thenReturn(userApiResponse);

            PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                    .rechargeId(rechargeId)
                    .transactionId(transactionId)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Execute
            rechargeSagaConsumer.consumePaymentApprovedEvent(event);

            // Verify: Should publish SAGA_PAYMENT_APPROVED log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());

            LogEvent sagaEvent = logEventCaptor.getValue();
            assertThat(sagaEvent).isNotNull();
            assertThat(sagaEvent.getServiceName()).isEqualTo("recharge-service");
            assertThat(sagaEvent.getLevel()).isEqualTo("INFO");
            assertThat(sagaEvent.getEventType()).isEqualTo("SAGA_PAYMENT_APPROVED");
            assertThat(sagaEvent.getMessage()).contains("SAGA", "Payment approved");
            assertThat(sagaEvent.getContext()).containsKeys("rechargeId", "userId", "transactionId", "previousStatus", "currentStatus", "amount");
            assertThat(sagaEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(sagaEvent.getContext().get("userId")).isEqualTo(userId.toString());
            assertThat(sagaEvent.getContext().get("transactionId")).isEqualTo(transactionId);
            assertThat(sagaEvent.getContext().get("previousStatus")).isEqualTo("PROCESSING");
            assertThat(sagaEvent.getContext().get("currentStatus")).isEqualTo("SUCCESS");
            assertThat(sagaEvent.getContext().get("amount")).isEqualTo(amount.toString());

            // Reset mocks for next iteration
            reset(rechargeRepository, rechargeEventProducer, userServiceClient, logEventPublisher);
        }
    }

    /**
     * Property: For any PaymentRejectedEvent, the system logs SAGA_PAYMENT_REJECTED event
     * with rechargeId, userId, failureReason, previousStatus, currentStatus, and amount.
     */
    @Test
    void property_paymentRejected_logsSagaEventWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random SAGA event data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            String failureReason = "Payment failed: " + random.nextInt(100);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));

            Recharge recharge = new Recharge();
            recharge.setId(random.nextLong(1, 100000));
            recharge.setRechargeId(rechargeId);
            recharge.setUserId(userId);
            recharge.setStatus(RechargeStatus.PROCESSING);
            recharge.setAmount(amount);

            when(rechargeRepository.findByRechargeId(rechargeId)).thenReturn(Optional.of(recharge));
            when(rechargeRepository.save(any(Recharge.class))).thenReturn(recharge);

            PaymentRejectedEvent event = PaymentRejectedEvent.builder()
                    .rechargeId(rechargeId)
                    .failureReason(failureReason)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Execute
            rechargeSagaConsumer.consumePaymentRejectedEvent(event);

            // Verify: Should publish SAGA_PAYMENT_REJECTED log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());

            LogEvent sagaEvent = logEventCaptor.getValue();
            assertThat(sagaEvent).isNotNull();
            assertThat(sagaEvent.getServiceName()).isEqualTo("recharge-service");
            assertThat(sagaEvent.getLevel()).isEqualTo("WARN");
            assertThat(sagaEvent.getEventType()).isEqualTo("SAGA_PAYMENT_REJECTED");
            assertThat(sagaEvent.getMessage()).contains("SAGA", "Payment rejected");
            assertThat(sagaEvent.getContext()).containsKeys("rechargeId", "userId", "failureReason", "previousStatus", "currentStatus", "amount");
            assertThat(sagaEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(sagaEvent.getContext().get("userId")).isEqualTo(userId.toString());
            assertThat(sagaEvent.getContext().get("failureReason")).isEqualTo(failureReason);
            assertThat(sagaEvent.getContext().get("previousStatus")).isEqualTo("PROCESSING");
            assertThat(sagaEvent.getContext().get("currentStatus")).isEqualTo("FAILED");
            assertThat(sagaEvent.getContext().get("amount")).isEqualTo(amount.toString());

            // Reset mocks for next iteration
            reset(rechargeRepository, logEventPublisher);
        }
    }

    /**
     * Property: For any SAGA event publication (RechargeInitiatedEvent, RechargeCompletedEvent),
     * the system logs SAGA_EVENT_PUBLISHED with eventType, rechargeId, routingKey, and exchange.
     */
    @Test
    void property_sagaEventPublication_logsEventWithRoutingDetails() {
        // This test verifies RechargeEventProducer logging
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random event data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));

            // Test RechargeInitiatedEvent logging
            com.omnicharge.common.event.saga.RechargeInitiatedEvent initiatedEvent = 
                    com.omnicharge.common.event.saga.RechargeInitiatedEvent.builder()
                    .rechargeId(rechargeId)
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod("UPI")
                    .mobileNumber("9876543210")
                    .operatorName("Airtel")
                    .planName("Unlimited")
                    .timestamp(LocalDateTime.now())
                    .build();

            RechargeEventProducer producer = new RechargeEventProducer(mock(org.springframework.amqp.rabbit.core.RabbitTemplate.class), logEventPublisher);
            producer.publishRechargeInitiated(initiatedEvent);

            // Verify: Should publish SAGA_EVENT_PUBLISHED log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());

            LogEvent publishedEvent = logEventCaptor.getValue();
            assertThat(publishedEvent).isNotNull();
            assertThat(publishedEvent.getServiceName()).isEqualTo("recharge-service");
            assertThat(publishedEvent.getLevel()).isEqualTo("INFO");
            assertThat(publishedEvent.getEventType()).isEqualTo("SAGA_EVENT_PUBLISHED");
            assertThat(publishedEvent.getMessage()).contains("SAGA event published");
            assertThat(publishedEvent.getContext()).containsKeys("eventType", "rechargeId", "routingKey", "exchange");
            assertThat(publishedEvent.getContext().get("eventType")).isEqualTo("RechargeInitiatedEvent");
            assertThat(publishedEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(publishedEvent.getContext().get("routingKey")).isEqualTo("saga.recharge.initiated");
            assertThat(publishedEvent.getContext().get("exchange")).isEqualTo("omnicharge.exchange");

            // Reset mocks for next iteration
            reset(logEventPublisher);
        }
    }
}
