package com.omnicharge.user.service;

import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.ForgotPasswordRequest;
import com.omnicharge.user.dto.ResetPasswordRequest;
import com.omnicharge.user.dto.VerifyOtpRequest;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService implements IPasswordResetService {

    private final UserRepository userRepository;
    private final IEmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final LogEventPublisher logEventPublisher;

    private static final long OTP_EXPIRATION_MINUTES = 5;

    public void forgotPassword(ForgotPasswordRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        // Verify auth provider is LOCAL
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password reset is only available for manual registration accounts");
        }

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Store OTP in Redis with 5-minute expiration
        String redisKey = "otp:" + request.getEmail();
        redisTemplate.opsForValue().set(
                redisKey,
                otp,
                OTP_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        // Send OTP via email
        emailService.sendOtpEmail(request.getEmail(), otp);

        log.info("OTP sent to email: {}", request.getEmail());
        
        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", user.getId());
        context.put("email", request.getEmail());
        publishBusinessLog("PASSWORD_RESET_REQUEST",
            "Password reset OTP requested: userId=" + user.getId(),
            context);
    }

    public boolean verifyOtp(VerifyOtpRequest request) {
        String redisKey = "otp:" + request.getEmail();
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            throw new BadRequestException("OTP expired or not found");
        }

        if (!storedOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        log.info("OTP verified successfully for email: {}", request.getEmail());
        return true;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Verify OTP first
        VerifyOtpRequest verifyRequest = new VerifyOtpRequest(request.getEmail(), request.getOtp());
        verifyOtp(verifyRequest);

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify auth provider is LOCAL
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password reset is only available for manual registration accounts");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete OTP from Redis
        String redisKey = "otp:" + request.getEmail();
        redisTemplate.delete(redisKey);

        log.info("Password reset successfully for email: {}", request.getEmail());
        
        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", user.getId());
        context.put("email", request.getEmail());
        publishBusinessLog("PASSWORD_RESET_COMPLETE",
            "Password reset completed: userId=" + user.getId(),
            context);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }
    
    // Helper method for business operation logging
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
}
