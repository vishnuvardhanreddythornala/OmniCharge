package com.omnicharge.notification.scheduler;

import com.omnicharge.notification.common.dto.ApiResponse;
import com.omnicharge.notification.client.RechargeServiceClient;
import com.omnicharge.notification.dto.ExpiringRechargeResponse;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.service.IEmailService;
import com.omnicharge.notification.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handles 5-day-before expiry REMINDERS only (EMAIL only — no SMS for warnings).
 * 
 * The actual expiry marking + instant expiry notifications (EMAIL + SMS) are now
 * handled by RechargeExpirySweeperTask (recharge-service) → PlanExpiryEventConsumer (this service).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanExpiryScheduler {

    private final RechargeServiceClient rechargeServiceClient;
    private final IEmailService emailService;
    private final INotificationService notificationService;

    // Runs daily at 8:00 AM
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkPlanExpiries() {
        log.info("Starting plan expiry reminder check (5-day warning, EMAIL only)...");
        checkExpiringPlans();
        log.info("Plan expiry reminder check completed");
    }

    private void checkExpiringPlans() {
        try {
            ApiResponse<List<ExpiringRechargeResponse>> response = 
                    rechargeServiceClient.getExpiringRecharges(5);
            
            List<ExpiringRechargeResponse> expiringRecharges = response.getData();
            
            if (expiringRecharges != null && !expiringRecharges.isEmpty()) {
                log.info("Found {} recharges expiring in 5 days", expiringRecharges.size());
                
                for (ExpiringRechargeResponse recharge : expiringRecharges) {
                    sendExpiryReminder(recharge);
                }
            } else {
                log.info("No recharges found expiring in 5 days");
            }
        } catch (Exception e) {
            log.error("Failed to check expiring plans", e);
        }
    }

    /**
     * 5-day warning: EMAIL ONLY (no SMS for reminders).
     * SMS is reserved for instant expiry notifications via PlanExpiryEventConsumer.
     */
    private void sendExpiryReminder(ExpiringRechargeResponse recharge) {
        try {
            if (recharge.getUserEmail() != null && !recharge.getUserEmail().isEmpty()) {
                // Send email
                emailService.sendPlanExpiryReminder(
                        recharge.getUserEmail(),
                        "User",
                        recharge.getOperatorName(),
                        recharge.getPlanName(),
                        recharge.getMobileNumber(),
                        5
                );

                // Save to DB for Dashboard Notifications tab
                String message = String.format(
                        "Your %s plan (%s) for %s will expire in 5 days. Recharge now to avoid interruption!",
                        recharge.getOperatorName(), recharge.getPlanName(), recharge.getMobileNumber()
                );
                notificationService.createAndSendEmail(
                        recharge.getUserId(),
                        recharge.getUserEmail(),
                        "Plan Expiry Reminder - 5 Days Left",
                        message,
                        NotificationCategory.PLAN_EXPIRY_REMINDER,
                        recharge.getRechargeId()
                );

                log.info("✅ 5-day expiry EMAIL reminder sent for recharge: {}", recharge.getRechargeId());
            } else {
                log.warn("No email address for userId: {}, skipping 5-day reminder", recharge.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to send expiry reminder for recharge: {}", recharge.getRechargeId(), e);
        }
    }
}
