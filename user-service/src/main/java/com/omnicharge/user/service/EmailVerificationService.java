package com.omnicharge.user.service;

import com.omnicharge.user.common.exception.BadRequestException;
import com.omnicharge.user.common.exception.DuplicateResourceException;
import com.omnicharge.user.common.exception.ResourceNotFoundException;
import com.omnicharge.user.common.logging.LogEvent;
import com.omnicharge.user.common.logging.LogEventPublisher;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender emailSender;
    private final LogEventPublisher logEventPublisher;

    private static final long OTP_EXPIRATION_MINUTES = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void sendVerificationOtp(Long userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRepository.existsByEmail(email) && (!email.equals(user.getEmail()) || Boolean.TRUE.equals(user.getIsEmailVerified()))) {
            throw new DuplicateResourceException("Email is already verified to another account");
        }

        String otp = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        String redisKey = "email-verify:" + userId;
        String emailRedisKey = "email-verify-address:" + userId;

        redisTemplate.opsForValue().set(redisKey, otp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(emailRedisKey, email, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Send email
        try {
            jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("OmniCharge - Email Verification");
            helper.setText(buildOtpVerificationEmailBody(otp), true);
            emailSender.send(message);
            log.info("Email verification OTP sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
            throw new BadRequestException("Failed to send verification email. Please check the address.");
        }

        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("email", email);
        publishBusinessLog("EMAIL_VERIFICATION_REQUESTED", "Email OTP requested", context);
    }

    @Transactional
    public void verifyEmail(Long userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String redisKey = "email-verify:" + userId;
        String emailRedisKey = "email-verify-address:" + userId;

        String storedOtp = redisTemplate.opsForValue().get(redisKey);
        String storedEmail = redisTemplate.opsForValue().get(emailRedisKey);

        if (storedOtp == null || storedEmail == null) {
            throw new BadRequestException("OTP expired or not found. Please request a new OTP.");
        }

        if (!storedOtp.equals(otp)) {
            throw new BadRequestException("Invalid OTP.");
        }

        if (userRepository.existsByEmail(storedEmail)) {
             userRepository.findByEmail(storedEmail).ifPresent(existingUser -> {
                 if (!existingUser.getId().equals(userId)) {
                      throw new DuplicateResourceException("Email is already used by another account");
                 }
             });
        }

        user.setEmail(storedEmail);
        user.setIsEmailVerified(true);
        userRepository.save(user);

        redisTemplate.delete(redisKey);
        redisTemplate.delete(emailRedisKey);

        // Send successful verification confirmation email
        try {
            jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(storedEmail);
            helper.setSubject("OmniCharge - Email Verified Successfully");
            helper.setText(buildVerificationSuccessEmailBody(), true);
            emailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send verification success email to {}", storedEmail, e);
        }

        log.info("Email verified successfully for user {}", userId);

        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("email", storedEmail);
        publishBusinessLog("EMAIL_VERIFIED", "Email successfully verified", context);
    }

    private void publishBusinessLog(String eventType, String message, Map<String, Object> context) {
        LogEvent logEvent = LogEvent.builder()
                .serviceName("user-service")
                .level("INFO")
                .logger(this.getClass().getName())
                .message(message)
                .eventType(eventType)
                .context(context)
                .timestamp(LocalDateTime.now())
                .build();
        logEventPublisher.publish(logEvent);
    }

    private String buildOtpVerificationEmailBody(String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #60a5fa, #5eead4); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                        .otp-box { background-color: #fff; border: 2px solid #5eead4; border-radius: 8px; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; font-family: monospace; letter-spacing: 5px; color: #0f172a; margin: 20px 0; }
                        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>OmniCharge</h1>
                        </div>
                        <div class="content">
                            <h2>Email Verification Request</h2>
                            <p>You have requested to verify your email address. Please use the following One Time Password (OTP) to proceed:</p>
                            <div class="otp-box">%s</div>
                            <p><strong>This OTP is valid for 10 minutes.</strong></p>
                            <p>If you did not request this verification, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 OmniCharge. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(otp);
    }

    private String buildVerificationSuccessEmailBody() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #4ade80, #14b8a6); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; text-align: center; }
                        .success-icon { font-size: 48px; margin-bottom: 10px; }
                        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>OmniCharge</h1>
                        </div>
                        <div class="content">
                            <div class="success-icon">✓</div>
                            <h2>Email Verified!</h2>
                            <p>Your email address has been successfully verified and securely linked to your OmniCharge account.</p>
                            <p>You will now receive all important updates, transaction receipts, and expiry notifications here.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 OmniCharge. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """;
    }
}
