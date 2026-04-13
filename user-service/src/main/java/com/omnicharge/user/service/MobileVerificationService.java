package com.omnicharge.user.service;

import com.omnicharge.user.common.exception.BadRequestException;
import com.omnicharge.user.common.exception.DuplicateResourceException;
import com.omnicharge.user.common.exception.ResourceNotFoundException;
import com.omnicharge.user.common.logging.LogEvent;
import com.omnicharge.user.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.AuthResponse;
import com.omnicharge.user.dto.OtpEvent;
import com.omnicharge.user.dto.SendMobileOtpRequest;
import com.omnicharge.user.dto.VerifyMobileOtpRequest;
import com.omnicharge.user.entity.RefreshToken;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.RefreshTokenRepository;
import com.omnicharge.user.repository.UserRepository;
import com.omnicharge.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MobileVerificationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final LogEventPublisher logEventPublisher;

    private static final long OTP_EXPIRATION_MINUTES = 5;
    private static final String EXCHANGE_NAME = "omnicharge.exchange";
    private static final String ROUTING_KEY_OTP = "mobile.otp.send";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void sendOtp(Long userId, SendMobileOtpRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user is already verified
        if (Boolean.TRUE.equals(user.getIsMobileVerified())) {
            throw new BadRequestException("Mobile number is already verified");
        }

        // CRITICAL: Check if mobile number is already used by another verified user
        userRepository.findByMobileNumber(request.getMobileNumber()).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(userId) && Boolean.TRUE.equals(existingUser.getIsMobileVerified())) {
                throw new DuplicateResourceException("This mobile number is already registered to another account");
            }
        });

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Store OTP in Redis
        String redisKey = "mobile-otp:" + userId;
        String mobileKey = "mobile-otp-num:" + userId;
        
        redisTemplate.opsForValue().set(redisKey, otp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(mobileKey, request.getMobileNumber(), OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Publish event to RabbitMQ for notification-service to pick up and send Twilio SMS
        OtpEvent event = OtpEvent.builder()
                .userId(userId)
                .mobileNumber(request.getMobileNumber())
                .otp(otp)
                .build();
                
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY_OTP, event);
        log.info("Mobile verification OTP sent to RabbitMQ for number: {}", request.getMobileNumber());
        
        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("mobileNumber", request.getMobileNumber());
        publishBusinessLog("MOBILE_OTP_REQUEST", "Mobile verification OTP requested", context);
    }

    @Transactional
    public AuthResponse verifyOtp(Long userId, VerifyMobileOtpRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String redisKey = "mobile-otp:" + userId;
        String mobileKey = "mobile-otp-num:" + userId;
        
        String storedOtp = redisTemplate.opsForValue().get(redisKey);
        String storedMobile = redisTemplate.opsForValue().get(mobileKey);

        if (storedOtp == null || storedMobile == null) {
            throw new BadRequestException("OTP expired or not found. Please request a new OTP.");
        }

        if (!storedMobile.equals(request.getMobileNumber())) {
            throw new BadRequestException("Mobile number mismatch. Please use the number you requested OTP for.");
        }

        if (!storedOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP. Please check and try again.");
        }

        // CRITICAL: Final uniqueness check before updating (race condition protection)
        userRepository.findByMobileNumber(request.getMobileNumber()).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(userId) && Boolean.TRUE.equals(existingUser.getIsMobileVerified())) {
                throw new DuplicateResourceException("This mobile number was just verified by another user. Please use a different number.");
            }
        });

        // OTP is valid. Update user with the verified mobile number (allows number override)
        user.setMobileNumber(request.getMobileNumber());
        user.setIsMobileVerified(true);
        userRepository.save(user);

        // Clear Redis keys
        redisTemplate.delete(redisKey);
        redisTemplate.delete(mobileKey);

        log.info("Mobile number verified successfully for user: {} with number: {}", userId, request.getMobileNumber());
        
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("mobileNumber", request.getMobileNumber());
        publishBusinessLog("MOBILE_VERIFIED", "Mobile verification successful", context);

        // Generate a fresh AuthResponse with updated isMobileVerified claim
        return generateFreshTokens(user);
    }

    private AuthResponse generateFreshTokens(User user) {
        boolean isProfileComplete = user.getMobileNumber() != null && !user.getMobileNumber().isEmpty();
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                isProfileComplete,
                user.getIsMobileVerified(),
                user.getAuthProvider().name()
        );

        String refreshTokenValue = jwtUtil.generateRefreshToken(user.getId());
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiration()));
        refreshTokenRepository.save(refreshToken);

        String redisKey = "refresh:" + user.getId() + ":" + refreshTokenValue;
        redisTemplate.opsForValue().set(
                redisKey,
                refreshTokenValue,
                jwtUtil.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
                .role(user.getRole())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .authProvider(user.getAuthProvider())
                .isProfileComplete(isProfileComplete)
                .isMobileVerified(user.getIsMobileVerified())
                .build();
    }

    private String generateOtp() {
        return String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
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
}
