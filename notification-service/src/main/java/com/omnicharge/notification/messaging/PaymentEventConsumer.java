package com.omnicharge.notification.messaging;

import com.omnicharge.common.event.PaymentCompletedEvent;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
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
public class PaymentEventConsumer {

    private final IEmailService emailService;
    private final ISmsService smsService;
    private final INotificationService notificationService;
    private final LogEventPublisher logEventPublisher;

    @RabbitListener(queues = "notification.payment.queue")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment completed event: {}", event.getTransactionId());
        log.info("Event details - userId: {}, email: {}, mobile: {}", 
                event.getUserId(), event.getUserEmail(), event.getUserMobile());

        // Log event consumption start
        publishBusinessLog("PAYMENT_EVENT_RECEIVED", "Payment event consumption started", Map.of(
                "transactionId", event.getTransactionId(),
                "userId", event.getUserId().toString(),
                "status", event.getStatus(),
                "amount", event.getAmount().toString()
        ));

        try {
            NotificationCategory category = "SUCCESS".equals(event.getStatus()) ?
                    NotificationCategory.PAYMENT_SUCCESS : NotificationCategory.PAYMENT_FAILED;

            // Send email notification
            if (event.getUserEmail() != null && !event.getUserEmail().isEmpty()) {
                log.info("Attempting to send payment email to: {}", event.getUserEmail());
                try {
                    emailService.sendPaymentConfirmation(event.getUserEmail(), event);
                    log.info("✅ Payment email sent successfully");
                } catch (Exception e) {
                    log.error("❌ Failed to send payment email to: {}", event.getUserEmail(), e);
                }
                
                log.info("Creating email notification record in database...");
                try {
                    notificationService.createAndSendEmail(
                            event.getUserId(),
                            event.getUserEmail(),
                            "Payment " + event.getStatus(),
                            "Payment confirmation email sent",
                            category,
                            event.getTransactionId()
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
                String smsMessage = buildPaymentSms(event);
                try {
                    notificationService.createAndSendSms(
                            event.getUserId(),
                            event.getUserMobile(),
                            smsMessage,
                            category,
                            event.getTransactionId()
                    );
                    log.info("✅ SMS notification created");
                } catch (Exception e) {
                    log.error("❌ Failed to create SMS notification", e);
                }
            } else {
                log.warn("⚠️  No mobile number in event, skipping SMS notification");
            }

            log.info("Payment notifications sent successfully for transaction: {}", event.getTransactionId());
            
            // Log event processing success
            publishBusinessLog("PAYMENT_EVENT_PROCESSED", "Payment event processed successfully", Map.of(
                    "transactionId", event.getTransactionId(),
                    "userId", event.getUserId().toString(),
                    "status", "SUCCESS",
                    "notificationsSent", (event.getUserEmail() != null ? "EMAIL " : "") + (event.getUserMobile() != null ? "SMS" : "")
            ));
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", event.getTransactionId(), e);
            
            // Log event processing failure
            publishBusinessLog("PAYMENT_EVENT_FAILED", "Payment event processing failed", Map.of(
                    "transactionId", event.getTransactionId(),
                    "userId", event.getUserId().toString(),
                    "status", "FAILED",
                    "errorMessage", e.getMessage()
            ));
        }
    }

    private String buildPaymentSms(PaymentCompletedEvent event) {
        String status = "SUCCESS".equals(event.getStatus()) ? "successful" : "failed";
        return String.format(
                "OmniCharge: Payment %s! TXN: %s, Amount: ₹%s, %s - %s for %s. Thank you!",
                status,
                event.getTransactionId(),
                event.getAmount(),
                event.getOperatorName(),
                event.getPlanName(),
                event.getMobileNumber()
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
