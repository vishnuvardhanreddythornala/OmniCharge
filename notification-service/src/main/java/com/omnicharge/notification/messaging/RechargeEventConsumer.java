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

@Component
@RequiredArgsConstructor
@Slf4j
public class RechargeEventConsumer {

    private final IEmailService emailService;
    private final ISmsService smsService;
    private final INotificationService notificationService;
    private final LogEventPublisher logEventPublisher;

    @RabbitListener(queues = "notification.recharge.queue")
    public void handleRechargeCompleted(RechargeCompletedEvent event) {
        log.info("Received recharge completed event: {}", event.getRechargeId());
        log.info("Event details - userId: {}, email: {}, mobile: {}", 
                event.getUserId(), event.getUserEmail(), event.getUserMobile());

        // Log event consumption start
        publishBusinessLog("RECHARGE_EVENT_RECEIVED", "Recharge event consumption started", Map.of(
                "rechargeId", event.getRechargeId(),
                "userId", event.getUserId().toString(),
                "status", event.getStatus(),
                "amount", event.getAmount().toString()
        ));

        try {
            NotificationCategory category = "SUCCESS".equals(event.getStatus()) ?
                    NotificationCategory.PAYMENT_SUCCESS : NotificationCategory.PAYMENT_FAILED;

            // Send email notification
            if (event.getUserEmail() != null && !event.getUserEmail().isEmpty()) {
                log.info("Attempting to send recharge email to: {}", event.getUserEmail());
                try {
                    emailService.sendRechargeConfirmation(event.getUserEmail(), event);
                    log.info("✅ Recharge email sent successfully");
                } catch (Exception e) {
                    log.error("❌ Failed to send recharge email to: {}", event.getUserEmail(), e);
                }
                
                log.info("Creating email notification record in database...");
                try {
                    notificationService.createAndSendEmail(
                            event.getUserId(),
                            event.getUserEmail(),
                            "Recharge " + event.getStatus(),
                            "Recharge confirmation email sent",
                            category,
                            event.getRechargeId()
                    );
                    log.info("✅ Email notification record created");
                } catch (Exception e) {
                    log.error("❌ Failed to create email notification record", e);
                }
            } else {
                log.warn("⚠️  No email address in event, skipping email notification");
            }

            // Send SMS notification
            if (event.getUserMobile() != null && !event.getUserMobile().isEmpty()) {
                log.info("Creating SMS notification for: {}", event.getUserMobile());
                String smsMessage = buildRechargeSms(event);
                try {
                    notificationService.createAndSendSms(
                            event.getUserId(),
                            event.getUserMobile(),
                            smsMessage,
                            category,
                            event.getRechargeId()
                    );
                    log.info("✅ SMS notification created");
                } catch (Exception e) {
                    log.error("❌ Failed to create SMS notification", e);
                }
            } else {
                log.warn("⚠️  No mobile number in event, skipping SMS notification");
            }

            log.info("Recharge notifications sent successfully for: {}", event.getRechargeId());
            
            // Log event processing success
            publishBusinessLog("RECHARGE_EVENT_PROCESSED", "Recharge event processed successfully", Map.of(
                    "rechargeId", event.getRechargeId(),
                    "userId", event.getUserId().toString(),
                    "status", "SUCCESS",
                    "notificationsSent", (event.getUserEmail() != null ? "EMAIL " : "") + (event.getUserMobile() != null ? "SMS" : "")
            ));
        } catch (Exception e) {
            log.error("Failed to process recharge event: {}", event.getRechargeId(), e);
            
            // Log event processing failure
            publishBusinessLog("RECHARGE_EVENT_FAILED", "Recharge event processing failed", Map.of(
                    "rechargeId", event.getRechargeId(),
                    "userId", event.getUserId().toString(),
                    "status", "FAILED",
                    "errorMessage", e.getMessage()
            ));
        }
    }

    private String buildRechargeSms(RechargeCompletedEvent event) {
        String status = "SUCCESS".equals(event.getStatus()) ? "successful" : "failed";
        return String.format(
                "OmniCharge: Recharge %s! ID: %s, %s - %s for %s, Amount: ₹%s. Thank you!",
                status,
                event.getRechargeId(),
                event.getOperatorName(),
                event.getPlanName(),
                event.getMobileNumber(),
                event.getAmount()
        );
    }

    /**
     * Helper method to publish business operation logs to centralized logging system
     */
    private void publishBusinessLog(String eventType, String message, Map<String, String> context) {
        try {
            LogEvent logEvent = new LogEvent();
            logEvent.setServiceName("notification-service");
            logEvent.setLevel("INFO");
            logEvent.setMessage(message);
            logEvent.setEventType(eventType);
            logEvent.setContext(new HashMap<>(context)); // Convert Map<String,String> to Map<String,Object>
            logEvent.setLogger(this.getClass().getName());
            logEvent.setTimestamp(java.time.LocalDateTime.now());
            
            logEventPublisher.publish(logEvent);
        } catch (Exception e) {
            log.error("Failed to publish business log for event: {}", eventType, e);
            // Don't throw - logging failure shouldn't break business operations
        }
    }
}
