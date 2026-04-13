package com.omnicharge.notification.messaging;

import com.omnicharge.notification.dto.OtpEvent;
import com.omnicharge.notification.service.ISmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpEventConsumer {

    private final ISmsService smsService;
    private final JavaMailSender emailSender;

    @RabbitListener(queues = "notification.otp.queue")
    public void consumeOtpEvent(OtpEvent event) {
        String recipient = event.getMobileNumber(); // The sender packages email or mobile inside this field
        log.info("Received OTP event for recipient: {}", recipient);
        
        try {
            if (recipient != null && recipient.contains("@")) {
                sendEmailOtp(recipient, event.getOtp());
            } else {
                String message = String.format("Your OmniCharge verification OTP is: %s. Valid for 5 minutes.", event.getOtp());
                smsService.sendSms(recipient, message);
                log.info("Successfully dispatched OTP verification to Twilio SMS");
            }
        } catch (Exception e) {
            log.error("Failed to send OTP to {}: {}", recipient, e.getMessage());
        }
    }

    private void sendEmailOtp(String email, String otp) throws Exception {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(email);
        helper.setSubject("OmniCharge - Secure Login OTP");
        
        String htmlBody = "<h2>Authentication Request</h2>" +
                          "<p>Use the following OTP to log into your account:</p>" +
                          "<h1 style='color: #4ade80; letter-spacing: 5px;'>" + otp + "</h1>" +
                          "<p>If you did not request this, please ignore it.</p>";
        helper.setText(htmlBody, true);
        emailSender.send(message);
        log.info("Successfully dispatched OTP verification to Email");
    }
}
