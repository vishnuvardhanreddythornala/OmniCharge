package com.omnicharge.notification.scheduler;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.notification.client.RechargeServiceClient;
import com.omnicharge.notification.dto.ExpiringRechargeResponse;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.service.IEmailService;
import com.omnicharge.notification.service.INotificationService;
import com.omnicharge.notification.service.ISmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanExpiryScheduler {

    private final RechargeServiceClient rechargeServiceClient;
    private final IEmailService emailService;
    private final ISmsService smsService;
    private final INotificationService notificationService;

    // Runs daily at 8:00 AM
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkPlanExpiries() {
        log.info("Starting plan expiry check...");

        checkExpiringPlans();
        checkExpiredPlans();

        log.info("Plan expiry check completed");
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
            }
        } catch (Exception e) {
            log.error("Failed to check expiring plans", e);
        }
    }

    private void checkExpiredPlans() {
        try {
            ApiResponse<List<ExpiringRechargeResponse>> response = 
                    rechargeServiceClient.getExpiredToday();
            
            List<ExpiringRechargeResponse> expiredRecharges = response.getData();
            
            if (expiredRecharges != null && !expiredRecharges.isEmpty()) {
                log.info("Found {} recharges expired today", expiredRecharges.size());
                
                for (ExpiringRechargeResponse recharge : expiredRecharges) {
                    sendExpiredNotification(recharge);
                    markRechargeAsExpired(recharge.getRechargeId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to check expired plans", e);
        }
    }

    private void sendExpiryReminder(ExpiringRechargeResponse recharge) {
        try {
            // Send email
            if (recharge.getUserEmail() != null && !recharge.getUserEmail().isEmpty()) {
                emailService.sendPlanExpiryReminder(
                        recharge.getUserEmail(),
                        "User",
                        recharge.getOperatorName(),
                        recharge.getPlanName(),
                        recharge.getMobileNumber(),
                        5
                );
                
                notificationService.createAndSendEmail(
                        recharge.getUserId(),
                        recharge.getUserEmail(),
                        "Plan Expiry Reminder",
                        "Your plan will expire in 5 days",
                        NotificationCategory.PLAN_EXPIRY_REMINDER,
                        recharge.getRechargeId()
                );
            }

            // Send SMS
            if (recharge.getUserMobile() != null && !recharge.getUserMobile().isEmpty()) {
                String smsMessage = String.format(
                        "OmniCharge: Your %s plan for %s expires in 5 days. Recharge now!",
                        recharge.getOperatorName(),
                        recharge.getMobileNumber()
                );
                
                smsService.sendSms(recharge.getUserMobile(), smsMessage);
                notificationService.createAndSendSms(
                        recharge.getUserId(),
                        recharge.getUserMobile(),
                        smsMessage,
                        NotificationCategory.PLAN_EXPIRY_REMINDER,
                        recharge.getRechargeId()
                );
            }

            log.info("Expiry reminder sent for recharge: {}", recharge.getRechargeId());
        } catch (Exception e) {
            log.error("Failed to send expiry reminder for recharge: {}", recharge.getRechargeId(), e);
        }
    }

    private void sendExpiredNotification(ExpiringRechargeResponse recharge) {
        try {
            // Send email
            if (recharge.getUserEmail() != null && !recharge.getUserEmail().isEmpty()) {
                emailService.sendPlanExpiredNotification(
                        recharge.getUserEmail(),
                        "User",
                        recharge.getOperatorName(),
                        recharge.getPlanName(),
                        recharge.getMobileNumber()
                );
                
                notificationService.createAndSendEmail(
                        recharge.getUserId(),
                        recharge.getUserEmail(),
                        "Plan Expired",
                        "Your plan has expired",
                        NotificationCategory.PLAN_EXPIRED,
                        recharge.getRechargeId()
                );
            }

            // Send SMS
            if (recharge.getUserMobile() != null && !recharge.getUserMobile().isEmpty()) {
                String smsMessage = String.format(
                        "OmniCharge: Your %s plan for %s has expired. Recharge now to continue services!",
                        recharge.getOperatorName(),
                        recharge.getMobileNumber()
                );
                
                smsService.sendSms(recharge.getUserMobile(), smsMessage);
                notificationService.createAndSendSms(
                        recharge.getUserId(),
                        recharge.getUserMobile(),
                        smsMessage,
                        NotificationCategory.PLAN_EXPIRED,
                        recharge.getRechargeId()
                );
            }

            log.info("Expired notification sent for recharge: {}", recharge.getRechargeId());
        } catch (Exception e) {
            log.error("Failed to send expired notification for recharge: {}", recharge.getRechargeId(), e);
        }
    }

    private void markRechargeAsExpired(String rechargeId) {
        try {
            rechargeServiceClient.markAsExpired(rechargeId);
            log.info("Marked recharge as expired: {}", rechargeId);
        } catch (Exception e) {
            log.error("Failed to mark recharge as expired: {}", rechargeId, e);
        }
    }
}
