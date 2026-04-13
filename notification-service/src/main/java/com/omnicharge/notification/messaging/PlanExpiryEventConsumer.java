package com.omnicharge.notification.messaging;

import com.omnicharge.notification.common.event.RechargeCompletedEvent;
import com.omnicharge.notification.common.logging.LogEvent;
import com.omnicharge.notification.common.logging.LogEventPublisher;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.service.IEmailService;
import com.omnicharge.notification.service.INotificationService;
import com.omnicharge.notification.service.ISmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumes plan expiry events published by the RechargeExpirySweeperTask.
 * Sends BOTH Email AND SMS notifications for instant expiry alerts.
 * All notifications are persisted to the database for the Dashboard Notifications tab.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanExpiryEventConsumer {

    private final IEmailService emailService;
    private final ISmsService smsService;
    private final INotificationService notificationService;
    private final LogEventPublisher logEventPublisher;

    @RabbitListener(queues = "notification.plan.expiry.queue")
    public void handlePlanExpired(RechargeCompletedEvent event) {
        log.info("Received plan.expiry event: rechargeId={}, userId={}, mobile={}", 
                event.getRechargeId(), event.getUserId(), event.getMobileNumber());

        try {
            // 1. Send EMAIL notification
            if (event.getUserEmail() != null && !event.getUserEmail().isEmpty()) {
                try {
                    emailService.sendPlanExpiredNotification(
                            event.getUserEmail(),
                            "User",
                            event.getOperatorName(),
                            event.getPlanName(),
                            event.getMobileNumber()
                    );
                    log.info("✅ Plan expiry email sent to: {}", event.getUserEmail());
                } catch (Exception e) {
                    log.error("❌ Failed to send plan expiry email to: {}", event.getUserEmail(), e);
                }

                // Save email notification to DB
                try {
                    String message = String.format(
                            "Your %s pack (%s) for %s has expired. Recharge now to continue services!",
                            event.getOperatorName(), event.getPlanName(), event.getMobileNumber()
                    );
                    notificationService.createAndSendEmail(
                            event.getUserId(),
                            event.getUserEmail(),
                            "Plan Expired - Recharge Now!",
                            message,
                            NotificationCategory.PLAN_EXPIRED,
                            event.getRechargeId()
                    );
                    log.info("✅ Plan expiry email notification saved to DB");
                } catch (Exception e) {
                    log.error("❌ Failed to save email notification record", e);
                }
            }

            if (event.getUserMobile() != null && !event.getUserMobile().isEmpty()) {
                try {
                    String smsMessage = String.format(
                            "OmniCharge: Your %s pack for %s has expired. Recharge now!",
                            event.getOperatorName(), event.getMobileNumber()
                    );

                    // Save SMS notification to DB
                    notificationService.createAndSendSms(
                            event.getUserId(),
                            event.getUserMobile(),
                            smsMessage,
                            NotificationCategory.PLAN_EXPIRED,
                            event.getRechargeId()
                    );
                    log.info("✅ Plan expiry SMS notification saved to DB");
                } catch (Exception e) {
                    log.error("❌ Failed to send plan expiry SMS to: {}", event.getUserMobile(), e);
                }
            }

            // Log business operation
            publishBusinessLog("PLAN_EXPIRY_NOTIFICATION_SENT", 
                    "Plan expiry notification sent (EMAIL + SMS)", Map.of(
                    "rechargeId", event.getRechargeId(),
                    "userId", event.getUserId().toString(),
                    "mobileNumber", event.getMobileNumber(),
                    "operatorName", event.getOperatorName(),
                    "planName", event.getPlanName()
            ));

        } catch (Exception e) {
            log.error("Failed to process plan.expiry event for rechargeId: {}", event.getRechargeId(), e);
        }
    }

    private void publishBusinessLog(String eventType, String message, Map<String, String> context) {
        try {
            LogEvent logEvent = new LogEvent();
            logEvent.setServiceName("notification-service");
            logEvent.setLevel("INFO");
            logEvent.setMessage(message);
            logEvent.setEventType(eventType);
            logEvent.setContext(new HashMap<>(context));
            logEvent.setLogger(this.getClass().getName());
            logEvent.setTimestamp(java.time.LocalDateTime.now());
            logEventPublisher.publish(logEvent);
        } catch (Exception e) {
            log.error("Failed to publish business log for event: {}", eventType, e);
        }
    }
}
