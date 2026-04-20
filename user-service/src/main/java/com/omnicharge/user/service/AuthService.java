package com.omnicharge.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.omnicharge.user.common.exception.BadRequestException;
import com.omnicharge.user.common.exception.ForbiddenException;
import com.omnicharge.user.common.exception.UnauthorizedException;
import com.omnicharge.user.common.logging.LogEvent;
import com.omnicharge.user.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.*;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.RefreshToken;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.RefreshTokenRepository;
import com.omnicharge.user.repository.UserRepository;
import com.omnicharge.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements InterfaceAuthService {

    private static final int MAX_DEVICES = 4;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final RedisTemplate<String, String> redisTemplate;
    private final LogEventPublisher logEventPublisher;
    private final RateLimitingService rateLimitingService;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    private static final long OTP_EXPIRATION_MINUTES = 5;
    private static final String EXCHANGE_NAME = "omnicharge.exchange";

    @Override
    @Transactional
    public AdminLoginInitResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Soft Migration check: Ignore password system for regular users
        if (user.getRole() != Role.ROLE_ADMIN) {
            throw new ForbiddenException("Password login is disabled for regular accounts. Please login via Mobile OTP or Google Auth.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            publishBusinessLog("ADMIN_LOGIN_PASSWORD_FAILED", "Failed Admin Password Check", Map.of("email", request.getEmail(), "ip", ipAddress));
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is disabled");
        }

        // Generate 2FA Email OTP
        String otp = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        String redisKey = "admin:2fa:otp:" + user.getEmail();
        redisTemplate.opsForValue().set(redisKey, otp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        
        // Log to console for dev
        log.warn("========= ADMIN 2FA OTP for {} → {} =========", user.getEmail(), otp);

        // RabbitMQ dispatch
        OtpEvent event = OtpEvent.builder().userId(user.getId()).mobileNumber(user.getEmail()).otp(otp).build();
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, "email.otp.send", event);

        publishBusinessLog("ADMIN_LOGIN_PASSWORD_SUCCESS", "Admin Password Validated. 2FA Initiated", Map.of("email", user.getEmail(), "ip", ipAddress));

        return AdminLoginInitResponse.builder()
                .requires2fa(true)
                .email(user.getEmail())
                .message("2FA OTP sent to your email")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyAdmin2fa(VerifyEmailOtpRequest request, String ipAddress) {
        String email = request.getEmail();
        String redisKey = "admin:2fa:otp:" + email;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UnauthorizedException("Session invalid"));

        if (storedOtp == null || !storedOtp.equals(request.getOtp())) {
            publishBusinessLog("ADMIN_LOGIN_2FA_FAILED", "Admin 2FA Failed", Map.of("email", email, "ip", ipAddress));
            throw new BadRequestException("Invalid or expired 2FA OTP.");
        }

        redisTemplate.delete(redisKey);
        publishBusinessLog("ADMIN_LOGIN_2FA_SUCCESS", "Admin fully authenticated", Map.of("email", email, "ip", ipAddress));

        return generateAuthResponse(user);
    }

    @Override
    public void sendEmailOtp(SendEmailOtpRequest request, String ipAddress) {
        String email = request.getEmail();
        
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getRole() == Role.ROLE_ADMIN) {
                throw new ForbiddenException("Administrators must use the secure Admin Portal to login.");
            }
        });

        rateLimitingService.checkAndIncrementSendOtp(email, ipAddress);

        String otp = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        String redisKey = "public:email:login:otp:" + email;
        redisTemplate.opsForValue().set(redisKey, otp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        log.warn("========= PUBLIC EMAIL OTP for {}  {} =========", email, otp);
        OtpEvent event = OtpEvent.builder().userId(0L).mobileNumber(email).otp(otp).build();
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, "email.otp.send", event);

        publishBusinessLog("PUBLIC_EMAIL_OTP_REQUEST", "Email OTP requested", Map.of("email", email, "ip", ipAddress));
    }

    @Override
    @Transactional
    public AuthResponse verifyEmailOtp(VerifyEmailOtpRequest request) {
        String email = request.getEmail();
        rateLimitingService.checkAndIncrementVerifyAttempts(email);

        String redisKey = "public:email:login:otp:" + email;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null || !storedOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid or expired OTP.");
        }

        rateLimitingService.resetVerifyAttempts(email);
        redisTemplate.delete(redisKey);

        // Auto-provision if doesn't exist
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName("User " + email.split("@")[0]);
            newUser.setAuthProvider(AuthProvider.LOCAL); // Using LOCAL to represent custom/direct rather than GOOGLE
            newUser.setRole(Role.ROLE_USER);
            newUser.setIsActive(true);
            newUser.setIsEmailVerified(true);
            newUser.setIsMobileVerified(false);
            return userRepository.save(newUser);
        });

        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new ForbiddenException("Administrators must use the secure Admin Portal to login.");
        }

        if (!user.getIsActive()) throw new UnauthorizedException("Account is disabled");

        publishBusinessLog("EMAIL_LOGIN_SUCCESS", "Email Login Successful", Map.of("email", email));
        return generateAuthResponse(user);
    }

    @Override
    public void sendMobileOtp(SendMobileOtpRequest request, String ipAddress) {
        String mobile = request.getMobileNumber();
        
        userRepository.findByMobileNumber(mobile).ifPresent(user -> {
            if (user.getRole() == Role.ROLE_ADMIN) {
                throw new ForbiddenException("Administrators must use the secure Admin Portal to login.");
            }
        });

        rateLimitingService.checkAndIncrementSendOtp(mobile, ipAddress);

        String otp = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        String redisKey = "public:mobile:login:otp:" + mobile;
        redisTemplate.opsForValue().set(redisKey, otp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        log.warn("========= PUBLIC MOBILE OTP for {} → {} =========", mobile, otp);
        OtpEvent event = OtpEvent.builder().userId(0L).mobileNumber(mobile).otp(otp).build();
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, "mobile.otp.send", event);

        publishBusinessLog("PUBLIC_MOBILE_OTP_REQUEST", "Mobile OTP requested", Map.of("mobileNumber", mobile, "ip", ipAddress));
    }

    @Override
    @Transactional
    public AuthResponse verifyMobileOtp(VerifyMobileOtpRequest request) {
        String mobile = request.getMobileNumber();
        rateLimitingService.checkAndIncrementVerifyAttempts(mobile);

        String redisKey = "public:mobile:login:otp:" + mobile;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null || !storedOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid or expired OTP.");
        }

        rateLimitingService.resetVerifyAttempts(mobile);
        redisTemplate.delete(redisKey);

        User user = userRepository.findByMobileNumber(mobile).orElseGet(() -> {
            User newUser = new User();
            newUser.setMobileNumber(mobile);
            newUser.setFullName("User " + mobile.substring(6));
            newUser.setAuthProvider(AuthProvider.MOBILE);
            newUser.setRole(Role.ROLE_USER);
            newUser.setIsActive(true);
            newUser.setIsMobileVerified(true);
            newUser.setIsEmailVerified(false);
            return userRepository.save(newUser);
        });

        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new ForbiddenException("Administrators must use the secure Admin Portal to login.");
        }

        if (!user.getIsActive()) throw new UnauthorizedException("Account is disabled");

        publishBusinessLog("MOBILE_LOGIN_SUCCESS", "Mobile Login Successful", Map.of("mobile", mobile));
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request, String ipAddress) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(request.getIdToken());
            if (idToken == null) throw new UnauthorizedException("Invalid Google ID token");

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            User user = userRepository.findByGoogleId(googleId).orElseGet(() -> {
                User newUser = new User();
                newUser.setGoogleId(googleId);
                newUser.setEmail(email);
                newUser.setFullName(name);
                newUser.setAuthProvider(AuthProvider.GOOGLE);
                newUser.setRole(Role.ROLE_USER);
                newUser.setIsActive(true);
                return userRepository.save(newUser);
            });

            if (!user.getIsActive()) throw new UnauthorizedException("Account is disabled");

            publishBusinessLog("GOOGLE_AUTH_SUCCESS", "Google Auth Successful", Map.of("email", email, "ip", ipAddress));
            return generateAuthResponse(user);

        } catch (Exception e) {
            throw new UnauthorizedException("Google authentication failed: " + e.getMessage());
        }
    }

    // Refresh and Logout logics kept strictly the same...
    
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = refreshToken.getUser();
        boolean isComplete = user.getMobileNumber() != null && !user.getMobileNumber().isEmpty();
        String newAccess = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(),
                isComplete, user.getIsMobileVerified(), user.getAuthProvider().name());

        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
                .role(user.getRole())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .authProvider(user.getAuthProvider())
                .isProfileComplete(isComplete)
                .isMobileVerified(user.getIsMobileVerified())
                .build();
    }

    @Transactional
    public void logout(String token) {
        try {
            String jti = jwtUtil.extractJti(token);
            Long remainingTime = jwtUtil.getRemainingExpiration(token);
            redisTemplate.opsForValue().set("blacklist:" + jti, "true", remainingTime, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {}
    }

    @Transactional
    public void logoutByRefreshToken(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(rt -> {
            redisTemplate.delete("refresh:" + rt.getUser().getId() + ":" + refreshTokenValue);
            refreshTokenRepository.delete(rt);
        });
    }

    private AuthResponse generateAuthResponse(User user) {
        boolean isComplete = user.getMobileNumber() != null && !user.getMobileNumber().isEmpty();
        String access = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(),
                isComplete, user.getIsMobileVerified(), user.getAuthProvider().name());
        
        long active = refreshTokenRepository.countByUser(user);
        if (active >= MAX_DEVICES) {
            java.util.List<RefreshToken> existing = refreshTokenRepository.findByUserOrderByExpiryDateAsc(user);
            int remove = (int) (active - MAX_DEVICES + 1);
            for (int i = 0; i < remove && i < existing.size(); i++) {
                RefreshToken oldest = existing.get(i);
                redisTemplate.delete("refresh:" + user.getId() + ":" + oldest.getToken());
                refreshTokenRepository.delete(oldest);
            }
        }

        String refresh = jwtUtil.generateRefreshToken(user.getId());
        RefreshToken rt = new RefreshToken();
        rt.setToken(refresh);
        rt.setUser(user);
        rt.setExpiryDate(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiration()));
        refreshTokenRepository.save(rt);

        redisTemplate.opsForValue().set("refresh:" + user.getId() + ":" + refresh, refresh, jwtUtil.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
                .role(user.getRole())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .authProvider(user.getAuthProvider())
                .isProfileComplete(isComplete)
                .isMobileVerified(user.getIsMobileVerified())
                .build();
    }

    private void publishBusinessLog(String type, String msg, Map<String, Object> ctx) {
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("user-service").level("INFO").logger(this.getClass().getName())
                .message(msg).eventType(type).context(ctx).timestamp(LocalDateTime.now()).build());
    }
}
