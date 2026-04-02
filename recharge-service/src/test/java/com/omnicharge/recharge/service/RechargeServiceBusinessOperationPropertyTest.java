package com.omnicharge.recharge.service;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.recharge.client.OperatorServiceClient;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.PlanResponse;
import com.omnicharge.recharge.dto.RechargeRequest;
import com.omnicharge.recharge.dto.UserProfileResponse;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.messaging.RechargeEventProducer;
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
import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Property-based test for recharge-service business operation logging.
 * 
 * Validates Property 33: Business Operation Event Logging
 * "For any critical business operation (user registration, recharge initiation,
 * payment processing, notification sending, plan activation/deactivation), the system
 * should log the event with relevant business context including entity IDs, amounts,
 * types, and statuses."
 * 
 * This test verifies that all critical business operations in recharge-service
 * (recharge initiation, status transitions, expiration) publish log events
 * with appropriate business context.
 * 
 * Feature: production-grade-centralized-logging, Property 33: Business Operation Event Logging
 * Validates: Requirements 15.2
 */
@ExtendWith(MockitoExtension.class)
@Tag("property-test")
public class RechargeServiceBusinessOperationPropertyTest {

    @Mock
    private RechargeRepository rechargeRepository;

    @Mock
    private OperatorServiceClient operatorServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private RechargeEventProducer rechargeEventProducer;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private RechargeService rechargeService;

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
    }

    /**
     * Property: For any recharge initiation, the system logs RECHARGE_INITIATED and RECHARGE_PROCESSING events
     * with rechargeId, userId, amount, operatorName, planName, mobileNumber, and status.
     */
    @Test
    void property_rechargeInitiation_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random recharge data
            Long userId = random.nextLong(1, 10000);
            Long operatorId = random.nextLong(1, 100);
            Long planId = random.nextLong(1, 1000);
            String mobileNumber = "9" + String.format("%09d", random.nextInt(1000000000));
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));
            String operatorName = "Operator-" + random.nextInt(10);
            String planName = "Plan-" + random.nextInt(100);
            int validityDays = random.nextInt(1, 365);

            // Setup mocks
            PlanResponse planResponse = PlanResponse.builder()
                    .id(planId)
                    .operatorId(operatorId)
                    .operatorName(operatorName)
                    .planName(planName)
                    .price(amount)
                    .validityDays(validityDays)
                    .isActive(true)
                    .build();

            ApiResponse<PlanResponse> planApiResponse = new ApiResponse<>(true, "Success", planResponse);
            when(operatorServiceClient.getPlan(planId)).thenReturn(planApiResponse);

            UserProfileResponse userProfile = UserProfileResponse.builder()
                    .id(userId)
                    .email("user" + userId + "@example.com")
                    .mobileNumber(mobileNumber)
                    .build();
            ApiResponse<UserProfileResponse> userApiResponse = new ApiResponse<>(true, "Success", userProfile);
            when(userServiceClient.getUserById(userId)).thenReturn(userApiResponse);

            Recharge savedRecharge = new Recharge();
            savedRecharge.setId(random.nextLong(1, 100000));
            savedRecharge.setRechargeId("OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            savedRecharge.setUserId(userId);
            savedRecharge.setMobileNumber(mobileNumber);
            savedRecharge.setOperatorId(operatorId);
            savedRecharge.setOperatorName(operatorName);
            savedRecharge.setPlanId(planId);
            savedRecharge.setPlanName(planName);
            savedRecharge.setAmount(amount);
            savedRecharge.setPlanValidityDays(validityDays);
            savedRecharge.setPlanExpiryDate(LocalDate.now().plusDays(validityDays));
            savedRecharge.setStatus(RechargeStatus.INITIATED);

            when(rechargeRepository.save(any(Recharge.class))).thenReturn(savedRecharge);

            RechargeRequest request = new RechargeRequest();
            request.setMobileNumber(mobileNumber);
            request.setOperatorId(operatorId);
            request.setPlanId(planId);
            request.setPaymentMethod("UPI");

            // Execute
            rechargeService.initiateRecharge(userId, request);

            // Verify: Should publish 2 log events (INITIATED and PROCESSING)
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(2)).publish(logEventCaptor.capture());

            // Verify RECHARGE_INITIATED event
            LogEvent initiatedEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "RECHARGE_INITIATED".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(initiatedEvent).isNotNull();
            assertThat(initiatedEvent.getServiceName()).isEqualTo("recharge-service");
            assertThat(initiatedEvent.getLevel()).isEqualTo("INFO");
            assertThat(initiatedEvent.getEventType()).isEqualTo("RECHARGE_INITIATED");
            assertThat(initiatedEvent.getContext()).containsKeys("rechargeId", "userId", "amount", "operatorName", "planName", "mobileNumber", "status");
            assertThat(initiatedEvent.getContext().get("userId")).isEqualTo(userId.toString());
            assertThat(initiatedEvent.getContext().get("amount")).isEqualTo(amount.toString());
            assertThat(initiatedEvent.getContext().get("operatorName")).isEqualTo(operatorName);
            assertThat(initiatedEvent.getContext().get("planName")).isEqualTo(planName);
            assertThat(initiatedEvent.getContext().get("mobileNumber")).isEqualTo(mobileNumber);
            assertThat(initiatedEvent.getContext().get("status")).isEqualTo("INITIATED");

            // Verify RECHARGE_PROCESSING event
            LogEvent processingEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "RECHARGE_PROCESSING".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(processingEvent).isNotNull();
            assertThat(processingEvent.getServiceName()).isEqualTo("recharge-service");
            assertThat(processingEvent.getLevel()).isEqualTo("INFO");
            assertThat(processingEvent.getEventType()).isEqualTo("RECHARGE_PROCESSING");
            assertThat(processingEvent.getContext()).containsKeys("rechargeId", "userId", "previousStatus", "currentStatus");
            assertThat(processingEvent.getContext().get("previousStatus")).isEqualTo("INITIATED");
            assertThat(processingEvent.getContext().get("currentStatus")).isEqualTo("PROCESSING");

            // Reset mocks for next iteration
            reset(rechargeRepository, operatorServiceClient, userServiceClient, rechargeEventProducer, logEventPublisher);
        }
    }

    /**
     * Property: For any recharge expiration, the system logs RECHARGE_EXPIRED event
     * with rechargeId, userId, and expiryDate.
     */
    @Test
    void property_rechargeExpiration_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random recharge data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            LocalDate expiryDate = LocalDate.now().minusDays(random.nextInt(1, 30));

            Recharge recharge = new Recharge();
            recharge.setId(random.nextLong(1, 100000));
            recharge.setRechargeId(rechargeId);
            recharge.setUserId(userId);
            recharge.setStatus(RechargeStatus.SUCCESS);
            recharge.setPlanExpiryDate(expiryDate);

            when(rechargeRepository.findByRechargeId(rechargeId)).thenReturn(Optional.of(recharge));
            when(rechargeRepository.save(any(Recharge.class))).thenReturn(recharge);

            // Execute
            rechargeService.markAsExpired(rechargeId);

            // Verify: Should publish RECHARGE_EXPIRED log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());

            LogEvent expiredEvent = logEventCaptor.getValue();
            assertThat(expiredEvent).isNotNull();
            assertThat(expiredEvent.getServiceName()).isEqualTo("recharge-service");
            assertThat(expiredEvent.getLevel()).isEqualTo("WARN");
            assertThat(expiredEvent.getEventType()).isEqualTo("RECHARGE_EXPIRED");
            assertThat(expiredEvent.getContext()).containsKeys("rechargeId", "userId", "expiryDate");
            assertThat(expiredEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(expiredEvent.getContext().get("userId")).isEqualTo(userId.toString());
            assertThat(expiredEvent.getContext().get("expiryDate")).isEqualTo(expiryDate.toString());

            // Reset mocks for next iteration
            reset(rechargeRepository, logEventPublisher);
        }
    }
}
