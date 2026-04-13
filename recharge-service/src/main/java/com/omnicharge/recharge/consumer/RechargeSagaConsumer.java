package com.omnicharge.recharge.consumer;

import com.omnicharge.recharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.recharge.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.recharge.common.event.RechargeCompletedEvent;
import com.omnicharge.recharge.common.logging.LogEvent;
import com.omnicharge.recharge.common.logging.LogEventPublisher;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.repository.RechargeRepository;
import com.omnicharge.recharge.messaging.RechargeEventProducer;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.UserProfileResponse;
import com.omnicharge.recharge.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RechargeSagaConsumer {

    private final RechargeRepository rechargeRepository;
    private final RechargeEventProducer rechargeEventProducer;
    private final UserServiceClient userServiceClient;
    private final LogEventPublisher logEventPublisher;

    @RabbitListener(queues = "saga.recharge.approved")
    @Transactional
    public void consumePaymentApprovedEvent(PaymentApprovedEvent event) {
        log.info("Saga Orchestrator: Consumed PaymentApprovedEvent for rechargeId: {}", event.getRechargeId());
        
        rechargeRepository.findByRechargeId(event.getRechargeId()).ifPresentOrElse(recharge -> {
            String previousStatus = recharge.getStatus().name();
            
            recharge.setStatus(RechargeStatus.SUCCESS);
            recharge.setTransactionId(event.getTransactionId());
            rechargeRepository.save(recharge);
            
            log.info("Recharge marked as SUCCESS for rechargeId: {}", event.getRechargeId());

            // Log business operation: SAGA_PAYMENT_APPROVED
            Map<String, Object> approvedContext = new HashMap<>();
            approvedContext.put("rechargeId", recharge.getRechargeId());
            approvedContext.put("userId", recharge.getUserId().toString());
            approvedContext.put("transactionId", event.getTransactionId());
            approvedContext.put("previousStatus", previousStatus);
            approvedContext.put("currentStatus", "SUCCESS");
            approvedContext.put("amount", recharge.getAmount().toString());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("recharge-service")
                    .level("INFO")
                    .message("SAGA: Payment approved, recharge completed successfully")
                    .eventType("SAGA_PAYMENT_APPROVED")
                    .context(approvedContext)
                    .timestamp(LocalDateTime.now())
                    .build());

            // Fetch user information to pass down to notifications.
            String userEmail = null;
            String userMobile = null;
            try {
                ApiResponse<UserProfileResponse> userApiResponse = userServiceClient.getUserById(recharge.getUserId());
                if (userApiResponse != null && userApiResponse.isSuccess() && userApiResponse.getData() != null) {
                    userEmail = userApiResponse.getData().getEmail();
                    userMobile = userApiResponse.getData().getMobileNumber();
                }
            } catch (Exception e) {
                log.error("Failed to fetch user details for notification logic: {}", recharge.getUserId(), e);
            }

            // Trigger Notification
            RechargeCompletedEvent completedEvent = RechargeCompletedEvent.builder()
                    .rechargeId(recharge.getRechargeId())
                    .userId(recharge.getUserId())
                    .userEmail(userEmail)
                    .userMobile(userMobile)
                    .mobileNumber(recharge.getMobileNumber())
                    .operatorName(recharge.getOperatorName())
                    .planName(recharge.getPlanName())
                    .amount(recharge.getAmount())
                    .status(recharge.getStatus().name())
                    .transactionId(recharge.getTransactionId())
                    .timestamp(LocalDateTime.now())
                    .build();

            rechargeEventProducer.publishRechargeCompleted(completedEvent);
        }, () -> log.error("Recharge not found for rechargeId: {}", event.getRechargeId()));
    }

    @RabbitListener(queues = "saga.recharge.rejected")
    @Transactional
    public void consumePaymentRejectedEvent(PaymentRejectedEvent event) {
        log.info("Saga Orchestrator: Consumed PaymentRejectedEvent for rechargeId: {}", event.getRechargeId());
        
        rechargeRepository.findByRechargeId(event.getRechargeId()).ifPresentOrElse(recharge -> {
            String previousStatus = recharge.getStatus().name();
            
            // CRITICAL FIX: If the recharge was already marked SUCCESS by a concurrent/previous webhook, 
            // DO NOT let a delayed "PENDING" transaction sweeper overwrite it back to FAILED!
            if (recharge.getStatus() == RechargeStatus.SUCCESS) {
                log.info("Ignoring duplicate PaymentRejectedEvent because recharge {} is ALREADY fully SUCCESS", event.getRechargeId());
                return;
            }
            
            recharge.setStatus(RechargeStatus.FAILED);
            recharge.setFailureReason(event.getFailureReason());
            rechargeRepository.save(recharge);
            
            log.warn("Recharge marked as FAILED for rechargeId: {}", event.getRechargeId());

            // Log business operation: SAGA_PAYMENT_REJECTED
            Map<String, Object> rejectedContext = new HashMap<>();
            rejectedContext.put("rechargeId", recharge.getRechargeId());
            rejectedContext.put("userId", recharge.getUserId().toString());
            rejectedContext.put("failureReason", event.getFailureReason());
            rejectedContext.put("previousStatus", previousStatus);
            rejectedContext.put("currentStatus", "FAILED");
            rejectedContext.put("amount", recharge.getAmount().toString());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("recharge-service")
                    .level("WARN")
                    .message("SAGA: Payment rejected, recharge failed")
                    .eventType("SAGA_PAYMENT_REJECTED")
                    .context(rejectedContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        }, () -> log.error("Recharge not found for rechargeId: {}", event.getRechargeId()));
    }
}
