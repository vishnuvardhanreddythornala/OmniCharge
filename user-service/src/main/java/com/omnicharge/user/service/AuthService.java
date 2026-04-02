package com.omnicharge.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.DuplicateResourceException;
import com.omnicharge.common.exception.UnauthorizedException;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final RedisTemplate<String, String> redisTemplate;
    private final LogEventPublisher logEventPublisher;

    @Transactional
    public void register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        // Check if mobile number already exists
        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateResourceException("Mobile number already registered");
        }

        // Create new user
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMobileNumber(request.getMobileNumber());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRole(Role.ROLE_USER);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", request.getEmail());
        
        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", savedUser.getId());
        context.put("email", savedUser.getEmail());
        context.put("authProvider", AuthProvider.LOCAL.name());
        context.put("role", Role.ROLE_USER.name());
        publishBusinessLog("USER_REGISTRATION", 
            "User registered: email=" + savedUser.getEmail() + ", provider=LOCAL", 
            context);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    // Log failed login attempt
                    Map<String, Object> context = new HashMap<>();
                    context.put("email", request.getEmail());
                    context.put("outcome", "FAILURE");
                    context.put("reason", "User not found");
                    publishBusinessLog("LOGIN_ATTEMPT",
                        "Login failed: email=" + request.getEmail() + ", reason=User not found",
                        context);
                    return new UnauthorizedException("Invalid email or password");
                });

        // Verify auth provider is LOCAL
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            // Log failed login attempt
            Map<String, Object> context = new HashMap<>();
            context.put("email", request.getEmail());
            context.put("outcome", "FAILURE");
            context.put("reason", "Wrong auth provider");
            publishBusinessLog("LOGIN_ATTEMPT",
                "Login failed: email=" + request.getEmail() + ", reason=Wrong auth provider",
                context);
            throw new BadRequestException("Please use Google Sign-In for this account");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Log failed login attempt
            Map<String, Object> context = new HashMap<>();
            context.put("userId", user.getId());
            context.put("email", request.getEmail());
            context.put("outcome", "FAILURE");
            context.put("reason", "Invalid password");
            publishBusinessLog("LOGIN_ATTEMPT",
                "Login failed: email=" + request.getEmail() + ", reason=Invalid password",
                context);
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check if user is active
        if (!user.getIsActive()) {
            // Log failed login attempt
            Map<String, Object> context = new HashMap<>();
            context.put("userId", user.getId());
            context.put("email", request.getEmail());
            context.put("outcome", "FAILURE");
            context.put("reason", "Account disabled");
            publishBusinessLog("LOGIN_ATTEMPT",
                "Login failed: email=" + request.getEmail() + ", reason=Account disabled",
                context);
            throw new UnauthorizedException("Account is disabled");
        }

        // Log successful login
        Map<String, Object> context = new HashMap<>();
        context.put("userId", user.getId());
        context.put("email", user.getEmail());
        context.put("outcome", "SUCCESS");
        context.put("authProvider", AuthProvider.LOCAL.name());
        publishBusinessLog("LOGIN_ATTEMPT",
            "Login successful: email=" + user.getEmail() + ", provider=LOCAL",
            context);

        // Generate tokens
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request) {
        log.info("=== Google Authentication Started ===");
        log.info("ID Token received (first 50 chars): {}", request.getIdToken().substring(0, Math.min(50, request.getIdToken().length())));
        
        try {
            // Verify Google ID token
            log.info("Verifying token with Google...");
            GoogleIdToken idToken = googleIdTokenVerifier.verify(request.getIdToken());
            
            if (idToken == null) {
                log.error("Token verification returned NULL - possible causes:");
                log.error("1. Token expired");
                log.error("2. Client ID mismatch");
                log.error("3. Network connectivity issue");
                log.error("4. Invalid token format");
                throw new UnauthorizedException("Invalid Google ID token");
            }

            log.info("Token verified successfully!");
            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            
            log.info("User info extracted - Email: {}, Name: {}, Google ID: {}", email, name, googleId);

            // Find or create user
            User user = userRepository.findByGoogleId(googleId)
                    .orElseGet(() -> {
                        log.info("New Google user, creating account...");
                        User newUser = createGoogleUser(googleId, email, name);
                        
                        // Log new Google user registration
                        Map<String, Object> regContext = new HashMap<>();
                        regContext.put("userId", newUser.getId());
                        regContext.put("email", newUser.getEmail());
                        regContext.put("authProvider", AuthProvider.GOOGLE.name());
                        regContext.put("role", Role.ROLE_USER.name());
                        publishBusinessLog("USER_REGISTRATION",
                            "User registered: email=" + newUser.getEmail() + ", provider=GOOGLE",
                            regContext);
                        
                        return newUser;
                    });

            // Check if user is active
            if (!user.getIsActive()) {
                log.warn("User account is disabled: {}", email);
                
                // Log failed OAuth attempt
                Map<String, Object> context = new HashMap<>();
                context.put("userId", user.getId());
                context.put("email", email);
                context.put("outcome", "FAILURE");
                context.put("reason", "Account disabled");
                publishBusinessLog("OAUTH_AUTHENTICATION",
                    "Google OAuth failed: email=" + email + ", reason=Account disabled",
                    context);
                
                throw new UnauthorizedException("Account is disabled");
            }

            // Log successful OAuth authentication
            Map<String, Object> context = new HashMap<>();
            context.put("userId", user.getId());
            context.put("email", user.getEmail());
            context.put("outcome", "SUCCESS");
            context.put("authProvider", AuthProvider.GOOGLE.name());
            publishBusinessLog("OAUTH_AUTHENTICATION",
                "Google OAuth successful: email=" + user.getEmail(),
                context);

            // Generate tokens
            AuthResponse response = generateAuthResponse(user);
            
            // Check if profile is complete (mobile number required)
            response.setIsProfileComplete(user.getMobileNumber() != null && !user.getMobileNumber().isEmpty());
            
            log.info("Google authentication successful for: {}", email);
            return response;

        } catch (GeneralSecurityException e) {
            log.error("Security exception during token verification: {}", e.getMessage());
            throw new UnauthorizedException("Google authentication failed: Security error");
        } catch (IOException e) {
            log.error("Network error during token verification: {}", e.getMessage());
            log.error("Check internet connectivity and firewall settings");
            throw new UnauthorizedException("Google authentication failed: Network error");
        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new UnauthorizedException("Google authentication failed: " + e.getMessage());
        }
    }

    private User createGoogleUser(String googleId, String email, String name) {
        // Check if email already exists with different auth provider
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered with manual registration");
        }

        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setFullName(name);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setRole(Role.ROLE_USER);
        user.setIsActive(true);
        user.setPassword(null); // No password for Google users

        return userRepository.save(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        boolean isProfileComplete = user.getMobileNumber() != null && !user.getMobileNumber().isEmpty();
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                isProfileComplete
        );

        // Generate refresh token
        String refreshTokenValue = jwtUtil.generateRefreshToken(user.getId());
        
        // Save refresh token to database
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiration()));
        refreshTokenRepository.save(refreshToken);

        // Store refresh token in Redis
        String redisKey = "refresh:" + user.getId();
        redisTemplate.opsForValue().set(
                redisKey,
                refreshTokenValue,
                jwtUtil.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        // Log token generation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", user.getId());
        context.put("email", user.getEmail());
        context.put("tokenType", "ACCESS_AND_REFRESH");
        publishBusinessLog("TOKEN_GENERATION",
            "Tokens generated for user: userId=" + user.getId(),
            context);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000) // in seconds
                .role(user.getRole())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .authProvider(user.getAuthProvider())
                .isProfileComplete(user.getMobileNumber() != null && !user.getMobileNumber().isEmpty())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Find refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Check if expired
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        // Verify Redis
        String redisKey = "refresh:" + refreshToken.getUser().getId();
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null || !storedToken.equals(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        User user = refreshToken.getUser();
        boolean isProfileComplete = user.getMobileNumber() != null && !user.getMobileNumber().isEmpty();
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                isProfileComplete
        );

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
                .role(user.getRole())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .authProvider(user.getAuthProvider())
                .isProfileComplete(user.getMobileNumber() != null && !user.getMobileNumber().isEmpty())
                .build();
    }

    @Transactional
    public void logout(String token) {
        try {
            // Extract JTI from token
            String jti = jwtUtil.extractJti(token);
            
            // Get remaining expiration time
            Long remainingTime = jwtUtil.getRemainingExpiration(token);
            
            // Add to blacklist in Redis
            String blacklistKey = "blacklist:" + jti;
            redisTemplate.opsForValue().set(
                    blacklistKey,
                    "true",
                    remainingTime,
                    TimeUnit.MILLISECONDS
            );
            
            log.info("Token blacklisted successfully");
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
            throw new BadRequestException("Failed to logout");
        }
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
