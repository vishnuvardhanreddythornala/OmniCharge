package com.omnicharge.notification.service;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService implements ISmsService {

    private final LogEventPublisher logEventPublisher;

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        // Initialize Twilio once when service starts
        Twilio.init(accountSid, authToken);
        log.info("Twilio SMS Service initialized with phone number: {}", fromNumber);
    }

    @Override
    public void sendSms(String mobileNumber, String message) {
        try {
            // Format mobile number for India (+91)
            String toNumber = mobileNumber.startsWith("+") ? mobileNumber : "+91" + mobileNumber;
            
            log.info("Sending SMS to: {}", toNumber);
            
            // Send SMS via Twilio
            Message twilioMessage = Message.creator(
                new PhoneNumber(toNumber),      // To (recipient)
                new PhoneNumber(fromNumber),    // From (Twilio number)
                message                         // Message body
            ).create();

            log.info("✅ SMS sent successfully!");
            log.info("   To: {}", toNumber);
            log.info("   SID: {}", twilioMessage.getSid());
            log.info("   Status: {}", twilioMessage.getStatus());
            log.info("   Message: {}", message);
            
            // Log business operation - SMS sent successfully
            publishBusinessLog("SMS_SENT", "SMS sent successfully via Twilio", Map.of(
                    "recipient", toNumber,
                    "twilioSid", twilioMessage.getSid(),
                    "twilioStatus", twilioMessage.getStatus().toString(),
                    "deliveryStatus", "SENT"
            ));
            
        } catch (Exception e) {
            log.error("❌ Failed to send SMS to: {}", mobileNumber, e);
            log.error("   Error: {}", e.getMessage());
            
            // Log business operation - SMS failed
            publishBusinessLog("SMS_FAILED", "SMS sending failed", Map.of(
                    "recipient", mobileNumber,
                    "deliveryStatus", "FAILED",
                    "errorMessage", e.getMessage()
            ));
            
            // Log but don't throw - notification failure shouldn't break the flow
            // In production, you might want to:
            // 1. Store failed SMS in database for retry
            // 2. Send alert to monitoring system
            // 3. Implement retry mechanism
        }
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
