package com.omnicharge.recharge.scheduler;

import com.omnicharge.recharge.common.dto.ApiResponse;
import com.omnicharge.recharge.common.event.RechargeCompletedEvent;
import com.omnicharge.recharge.common.logging.LogEvent;
import com.omnicharge.recharge.common.logging.LogEventPublisher;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.UserProfileResponse;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.messaging.RechargeEventProducer;
import com.omnicharge.recharge.repository.RechargeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Downtime-Resilient Recharge Expiry Sweeper.
 * 
 * Runs every hour. Queries for ALL recharges where:
 *   status = SUCCESS AND planExpiryDate <= TODAY
 * 
 * This means if the server is offline for 2 days, upon restart
 * this query catches ALL past-due expirations it missed.
 * 
 * For each expired recharge:
 * 1. Marks status as EXPIRED in the database
 * 2. Publishes a RechargeCompletedEvent (status=EXPIRED) to RabbitMQ 
 *    with routing key "plan.expiry" so notification-service can send EMAIL + SMS
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RechargeExpirySweeperTask {

    private final RechargeRepository rechargeRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UserServiceClient userServiceClient;
    private final LogEventPublisher logEventPublisher;

    /**
     * Runs every hour (at minute 0).
     * Example: 12:00, 13:00, 14:00, etc.
     */
    @Scheduled(cron = "0 */1 * * * ?")
    @Transactional
    public void sweepExpiredRecharges() {
        LocalDate today = LocalDate.now();
        log.info("RECHARGE_EXPIRY_SWEEPER: Starting sweep at {} (cutoff date: {})", LocalDateTime.now(), today);

        List<Recharge> expiredRecharges = rechargeRepository.findByStatusAndPlanExpiryDateBefore(
                RechargeStatus.SUCCESS, today);

        if (expiredRecharges.isEmpty()) {
            log.info("RECHARGE_EXPIRY_SWEEPER: No expired recharges found. Sweep complete.");
            return;
        }

        log.info("RECHARGE_EXPIRY_SWEEPER: Found {} recharges to expire", expiredRecharges.size());
        int successCount = 0;
        int failCount = 0;

        for (Recharge recharge : expiredRecharges) {
            try {
                // 1. Mark as EXPIRED
                recharge.setStatus(RechargeStatus.EXPIRED);
                rechargeRepository.save(recharge);

                // 2. Fetch user details for notification
                String userEmail = null;
                String userMobile = null;
                try {
                    ApiResponse<UserProfileResponse> userRes = userServiceClient.getUserById(recharge.getUserId());
                    if (userRes != null && userRes.getData() != null) {
                        userEmail = userRes.getData().getEmail();
                        userMobile = userRes.getData().getMobileNumber();
                    }
                } catch (Exception e) {
                    log.warn("RECHARGE_EXPIRY_SWEEPER: Could not fetch user {} for notification enrichment", 
                            recharge.getUserId());
                }

                // 3. Publish plan.expiry event to RabbitMQ for notification-service
                RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                        .rechargeId(recharge.getRechargeId())
                        .userId(recharge.getUserId())
                        .userEmail(userEmail)
                        .userMobile(userMobile)
                        .mobileNumber(recharge.getMobileNumber())
                        .operatorName(recharge.getOperatorName())
                        .planName(recharge.getPlanName())
                        .amount(recharge.getAmount())
                        .status("EXPIRED")
                        .transactionId(recharge.getTransactionId())
                        .timestamp(LocalDateTime.now())
                        .build();

                rabbitTemplate.convertAndSend("omnicharge.exchange", "plan.expiry", event);

                log.info("RECHARGE_EXPIRY_SWEEPER: Expired rechargeId={}, userId={}, plan={}, expiryDate={}", 
                        recharge.getRechargeId(), recharge.getUserId(), 
                        recharge.getPlanName(), recharge.getPlanExpiryDate());
                successCount++;

            } catch (Exception e) {
                log.error("RECHARGE_EXPIRY_SWEEPER: Failed to expire rechargeId={}", recharge.getRechargeId(), e);
                failCount++;
            }
        }

        log.info("RECHARGE_EXPIRY_SWEEPER: Sweep complete. Expired: {}, Failed: {}", successCount, failCount);

        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("cutoffDate", today.toString());
        context.put("totalFound", String.valueOf(expiredRecharges.size()));
        context.put("successCount", String.valueOf(successCount));
        context.put("failCount", String.valueOf(failCount));

        logEventPublisher.publish(LogEvent.builder()
                .serviceName("recharge-service")
                .level(failCount > 0 ? "WARN" : "INFO")
                .message("Recharge expiry sweep completed")
                .eventType("RECHARGE_EXPIRY_SWEEP")
                .context(context)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
