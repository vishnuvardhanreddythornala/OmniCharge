package com.omnicharge.notification.service;

import com.omnicharge.notification.common.event.PaymentCompletedEvent;
import com.omnicharge.notification.common.event.RechargeCompletedEvent;
import com.omnicharge.notification.common.logging.LogEvent;
import com.omnicharge.notification.common.logging.LogEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;
    private final LogEventPublisher logEventPublisher;

    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${spring.mail.password}")
    private String mailPassword;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("Email Service initialized");
        log.info("From Email: {}", fromEmail);
        log.info("Password length: {}", mailPassword != null ? mailPassword.length() : 0);
        log.info("Password first 4 chars: {}", mailPassword != null && mailPassword.length() >= 4 ? mailPassword.substring(0, 4) + "..." : "N/A");
    }

    @Override
    public void sendPaymentConfirmation(String toEmail, PaymentCompletedEvent event) {
        try {
            String subject = "OmniCharge - Payment " + event.getStatus();
            String htmlBody = buildPaymentConfirmationEmail(event);

            sendHtmlEmail(toEmail, subject, htmlBody);
            log.info("Payment confirmation email sent to: {}", toEmail);
            
            // Log business operation - Email sent successfully
            publishBusinessLog("EMAIL_SENT", "Payment confirmation email sent successfully", Map.of(
                    "recipient", toEmail,
                    "emailType", "PAYMENT_CONFIRMATION",
                    "transactionId", event.getTransactionId(),
                    "deliveryStatus", "SENT"
            ));
        } catch (Exception e) {
            log.error("Failed to send payment confirmation email to: {}", toEmail, e);
            
            // Log business operation - Email failed
            publishBusinessLog("EMAIL_FAILED", "Payment confirmation email sending failed", Map.of(
                    "recipient", toEmail,
                    "emailType", "PAYMENT_CONFIRMATION",
                    "transactionId", event.getTransactionId(),
                    "deliveryStatus", "FAILED",
                    "errorMessage", e.getMessage()
            ));
            
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendRechargeConfirmation(String toEmail, RechargeCompletedEvent event) {
        try {
            String subject = "OmniCharge - Recharge " + event.getStatus();
            String htmlBody = buildRechargeConfirmationEmail(event);

            sendHtmlEmail(toEmail, subject, htmlBody);
            log.info("Recharge confirmation email sent to: {}", toEmail);
            
            // Log business operation - Email sent successfully
            publishBusinessLog("EMAIL_SENT", "Recharge confirmation email sent successfully", Map.of(
                    "recipient", toEmail,
                    "emailType", "RECHARGE_CONFIRMATION",
                    "rechargeId", event.getRechargeId(),
                    "deliveryStatus", "SENT"
            ));
        } catch (Exception e) {
            log.error("Failed to send recharge confirmation email to: {}", toEmail, e);
            
            // Log business operation - Email failed
            publishBusinessLog("EMAIL_FAILED", "Recharge confirmation email sending failed", Map.of(
                    "recipient", toEmail,
                    "emailType", "RECHARGE_CONFIRMATION",
                    "rechargeId", event.getRechargeId(),
                    "deliveryStatus", "FAILED",
                    "errorMessage", e.getMessage()
            ));
            
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendPlanExpiryReminder(String toEmail, String userName, String operatorName, 
                                       String planName, String mobileNumber, int daysLeft) {
        try {
            String subject = "OmniCharge - Plan Expiry Reminder";
            String htmlBody = buildPlanExpiryReminderEmail(userName, operatorName, planName, mobileNumber, daysLeft);

            sendHtmlEmail(toEmail, subject, htmlBody);
            log.info("Plan expiry reminder email sent to: {}", toEmail);
            
            // Log business operation - Email sent successfully
            publishBusinessLog("EMAIL_SENT", "Plan expiry reminder email sent successfully", Map.of(
                    "recipient", toEmail,
                    "emailType", "PLAN_EXPIRY_REMINDER",
                    "operatorName", operatorName,
                    "mobileNumber", mobileNumber,
                    "daysLeft", String.valueOf(daysLeft),
                    "deliveryStatus", "SENT"
            ));
        } catch (Exception e) {
            log.error("Failed to send plan expiry reminder email to: {}", toEmail, e);
            
            // Log business operation - Email failed
            publishBusinessLog("EMAIL_FAILED", "Plan expiry reminder email sending failed", Map.of(
                    "recipient", toEmail,
                    "emailType", "PLAN_EXPIRY_REMINDER",
                    "operatorName", operatorName,
                    "mobileNumber", mobileNumber,
                    "deliveryStatus", "FAILED",
                    "errorMessage", e.getMessage()
            ));
            
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendPlanExpiredNotification(String toEmail, String userName, String operatorName, 
                                            String planName, String mobileNumber) {
        try {
            String subject = "OmniCharge - Plan Expired";
            String htmlBody = buildPlanExpiredEmail(userName, operatorName, planName, mobileNumber);

            sendHtmlEmail(toEmail, subject, htmlBody);
            log.info("Plan expired notification email sent to: {}", toEmail);
            
            // Log business operation - Email sent successfully
            publishBusinessLog("EMAIL_SENT", "Plan expired notification email sent successfully", Map.of(
                    "recipient", toEmail,
                    "emailType", "PLAN_EXPIRED",
                    "operatorName", operatorName,
                    "mobileNumber", mobileNumber,
                    "deliveryStatus", "SENT"
            ));
        } catch (Exception e) {
            log.error("Failed to send plan expired notification email to: {}", toEmail, e);
            
            // Log business operation - Email failed
            publishBusinessLog("EMAIL_FAILED", "Plan expired notification email sending failed", Map.of(
                    "recipient", toEmail,
                    "emailType", "PLAN_EXPIRED",
                    "operatorName", operatorName,
                    "mobileNumber", mobileNumber,
                    "deliveryStatus", "FAILED",
                    "errorMessage", e.getMessage()
            ));
            
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
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

    private String buildPaymentConfirmationEmail(PaymentCompletedEvent event) {
        String statusIcon = "";
        String statusColor = "SUCCESS".equals(event.getStatus()) ? "#28a745" : "#dc3545";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");
        String formattedDate = event.getTimestamp().format(formatter);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }
                    .status { font-size: 24px; font-weight: bold; color: %s; margin: 20px 0; }
                    table { width: 100%%; border-collapse: collapse; margin: 20px 0; background: white; border-radius: 8px; overflow: hidden; }
                    td { padding: 12px; border-bottom: 1px solid #dee2e6; }
                    td:first-child { font-weight: bold; width: 40%%; background: #f8f9fa; }
                    .footer { text-align: center; margin-top: 30px; color: #6c757d; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>OmniCharge</h1>
                        <p>Payment Confirmation</p>
                    </div>
                    <div class="content">
                        <div class="status">%s Payment %s</div>
                        <table>
                            <tr><td>Transaction ID</td><td>%s</td></tr>
                            <tr><td>Recharge ID</td><td>%s</td></tr>
                            <tr><td>Mobile Number</td><td>%s</td></tr>
                            <tr><td>Operator</td><td>%s</td></tr>
                            <tr><td>Plan</td><td>%s</td></tr>
                            <tr><td>Amount</td><td>₹%s</td></tr>
                            <tr><td>Payment Method</td><td>%s</td></tr>
                            <tr><td>Date & Time</td><td>%s</td></tr>
                        </table>
                        <p>Thank you for using OmniCharge!</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated email. Please do not reply.</p>
                        <p>&copy; 2026 OmniCharge. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                statusColor,
                statusIcon,
                event.getStatus(),
                event.getTransactionId(),
                event.getRechargeId(),
                event.getMobileNumber(),
                event.getOperatorName(),
                event.getPlanName(),
                event.getAmount(),
                event.getPaymentMethod(),
                formattedDate
            );
    }

    private String buildRechargeConfirmationEmail(RechargeCompletedEvent event) {
        String statusIcon = "";
        String statusColor = "SUCCESS".equals(event.getStatus()) ? "#28a745" : "#dc3545";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");
        String formattedDate = event.getTimestamp().format(formatter);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }
                    .status { font-size: 24px; font-weight: bold; color: %s; margin: 20px 0; }
                    table { width: 100%%; border-collapse: collapse; margin: 20px 0; background: white; border-radius: 8px; overflow: hidden; }
                    td { padding: 12px; border-bottom: 1px solid #dee2e6; }
                    td:first-child { font-weight: bold; width: 40%%; background: #f8f9fa; }
                    .footer { text-align: center; margin-top: 30px; color: #6c757d; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>OmniCharge</h1>
                        <p>Recharge Confirmation</p>
                    </div>
                    <div class="content">
                        <div class="status">%s Recharge %s</div>
                        <table>
                            <tr><td>Recharge ID</td><td>%s</td></tr>
                            <tr><td>Mobile Number</td><td>%s</td></tr>
                            <tr><td>Operator</td><td>%s</td></tr>
                            <tr><td>Plan</td><td>%s</td></tr>
                            <tr><td>Amount</td><td>₹%s</td></tr>
                            <tr><td>Transaction ID</td><td>%s</td></tr>
                            <tr><td>Date & Time</td><td>%s</td></tr>
                        </table>
                        <p>Thank you for using OmniCharge!</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated email. Please do not reply.</p>
                        <p>&copy; 2026 OmniCharge. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                statusColor,
                statusIcon,
                event.getStatus(),
                event.getRechargeId(),
                event.getMobileNumber(),
                event.getOperatorName(),
                event.getPlanName(),
                event.getAmount(),
                event.getTransactionId(),
                formattedDate
            );
    }

    private String buildPlanExpiryReminderEmail(String userName, String operatorName, 
                                                String planName, String mobileNumber, int daysLeft) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }
                    .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .footer { text-align: center; margin-top: 30px; color: #6c757d; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Plan Expiry Reminder</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <div class="warning">
                            <strong>Your %s plan for mobile number %s will expire in %d days.</strong>
                        </div>
                        <p><strong>Plan Details:</strong></p>
                        <ul>
                            <li>Operator: %s</li>
                            <li>Plan: %s</li>
                            <li>Mobile: %s</li>
                        </ul>
                        <p>Recharge now to avoid service interruption!</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 OmniCharge. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, operatorName, mobileNumber, daysLeft, operatorName, planName, mobileNumber);
    }

    private String buildPlanExpiredEmail(String userName, String operatorName, 
                                        String planName, String mobileNumber) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #fa709a 0%%, #fee140 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }
                    .alert { background: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; border-radius: 4px; color: #721c24; }
                    .footer { text-align: center; margin-top: 30px; color: #6c757d; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Plan Expired</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <div class="alert">
                            <strong>Your %s plan for mobile number %s has expired.</strong>
                        </div>
                        <p><strong>Plan Details:</strong></p>
                        <ul>
                            <li>Operator: %s</li>
                            <li>Plan: %s</li>
                            <li>Mobile: %s</li>
                        </ul>
                        <p>Recharge now to continue enjoying uninterrupted services!</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 OmniCharge. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, operatorName, mobileNumber, operatorName, planName, mobileNumber);
    }
}
