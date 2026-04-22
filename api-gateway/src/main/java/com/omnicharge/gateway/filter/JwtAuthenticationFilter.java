package com.omnicharge.gateway.filter;

import com.omnicharge.gateway.common.logging.LogEvent;
import com.omnicharge.gateway.common.logging.LogEventPublisher;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final LogEventPublisher logEventPublisher;

    @PostConstruct
    public void init() {
        log.info("JWT secret loaded in gateway, length: {}",
                jwtSecret != null ? jwtSecret.length() : "NULL");
    }

    private static final String JWT_HEADER = "Authorization";
    private static final String JWT_TOKEN_PREFIX = "Bearer ";
    private static final String JWT_CLAIM_USER_ID = "userId";
    private static final String JWT_CLAIM_ROLE = "role";
    private static final String JWT_CLAIM_EMAIL = "email";
    private static final String JWT_CLAIM_JTI = "jti";
    private static final String JWT_CLAIM_PROFILE_COMPLETE = "isProfileComplete";

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/google",
            "/api/auth/refresh-token",
            "/api/auth/logout",
            "/api/auth/forgot-password",
            "/api/auth/verify-otp",
            "/api/auth/reset-password",
            "/api/auth/mobile",
            "/api/auth/admin/verify-2fa",
            "/api/auth/email/send-login-otp",
            "/api/auth/email/verify-login-otp",
            "/api/operators/detect",
            "/api/operators/active",
            "/api/operators/",
            "/api/plans/",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/webjars"
    );
    
    private static final List<String> PROFILE_INCOMPLETE_ALLOWED_PATHS = Arrays.asList(
            "/api/users/profile",
            "/api/users/mobile-otp/send",
            "/api/users/mobile-otp/verify",
            "/api/recharges/history",
            "/api/payments/user",
            "/api/notifications",
            "/api/auth/logout"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract JWT token
        String authHeader = request.getHeaders().getFirst(JWT_HEADER);
        log.debug("Auth Header present: {}", authHeader != null);
        
        if (authHeader == null || !authHeader.startsWith(JWT_TOKEN_PREFIX)) {
            log.debug("Missing or invalid Authorization header for path: {}", path);
            logAuthenticationFailure(path, "MISSING_OR_INVALID_HEADER", 
                "Authorization header is missing or does not start with 'Bearer '", null);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(JWT_TOKEN_PREFIX.length());
        log.debug("Token extracted, length: {}", token.length());

        try {
            // Validate JWT token
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Extract JTI for blacklist check
            String jti = claims.get(JWT_CLAIM_JTI, String.class);
            String blacklistKey = "blacklist:" + jti;
            
            // Extract user info for logging
            String userId = claims.get(JWT_CLAIM_USER_ID, String.class);
            String email = claims.get(JWT_CLAIM_EMAIL, String.class);

            // Profile Completion Enforcement
            Boolean isProfileComplete = claims.get(JWT_CLAIM_PROFILE_COMPLETE, Boolean.class);
            if (Boolean.FALSE.equals(isProfileComplete) && !isProfileIncompleteAllowedPath(path)) {
                log.debug("Blocked access: incomplete profile for path: {}", path);
                logAuthenticationFailure(path, "INCOMPLETE_PROFILE", 
                    "User profile is incomplete. Access restricted to profile completion endpoints only.", 
                    sanitizeTokenForLogging(token, userId, email));
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // Check if token is blacklisted in Redis
            return reactiveRedisTemplate.hasKey(blacklistKey)
                    .flatMap(isBlacklisted -> {
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            // Token is blacklisted (user logged out)
                            logAuthenticationFailure(path, "BLACKLISTED_TOKEN", 
                                "Token has been blacklisted (user logged out)", 
                                sanitizeTokenForLogging(token, userId, email));
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        // Token is valid, add user info to headers for downstream services
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header("X-User-Id", claims.get(JWT_CLAIM_USER_ID, String.class))
                                .header("X-User-Role", claims.get(JWT_CLAIM_ROLE, String.class))
                                .header("X-User-Email", claims.get(JWT_CLAIM_EMAIL, String.class))
                                .build();
                        
                        // Log successful authentication at DEBUG level
                        logSuccessfulAuthentication(path, userId, email);

                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    })
                    .onErrorResume(e -> {
                        // Redis error - allow request to proceed (fail open)
                        log.warn("Redis error during blacklist check, allowing request: {}", e.getMessage());
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header("X-User-Id", claims.get(JWT_CLAIM_USER_ID, String.class))
                                .header("X-User-Role", claims.get(JWT_CLAIM_ROLE, String.class))
                                .header("X-User-Email", claims.get(JWT_CLAIM_EMAIL, String.class))
                                .build();

                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    });

        } catch (Exception e) {
            log.error("JWT validation failed for path {}: {} - {}", path, e.getClass().getSimpleName(), e.getMessage());
            logAuthenticationFailure(path, e.getClass().getSimpleName(), 
                e.getMessage(), sanitizeTokenForLogging(token, null, null));
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private boolean isProfileIncompleteAllowedPath(String path) {
        return PROFILE_INCOMPLETE_ALLOWED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -1;
    }
    
    /**
     * Logs authentication failures with sanitized token details.
     * Excludes sensitive data but includes enough context for debugging.
     */
    private void logAuthenticationFailure(String path, String failureReason, 
                                         String failureMessage, Map<String, Object> tokenDetails) {
        String message = String.format("[AUTH-FAILURE] Path: %s | Reason: %s | Message: %s", 
            path, failureReason, failureMessage);
        
        Map<String, Object> context = new HashMap<>();
        context.put("path", path);
        context.put("failureReason", failureReason);
        context.put("failureMessage", failureMessage);
        
        if (tokenDetails != null) {
            context.putAll(tokenDetails);
        }
        
        LogEvent logEvent = LogEvent.builder()
                .serviceName("api-gateway")
                .level("WARN")
                .eventType("AUTHENTICATION")
                .logger(JwtAuthenticationFilter.class.getName())
                .message(message)
                .timestamp(LocalDateTime.now())
                .threadName(Thread.currentThread().getName())
                .context(context)
                .build();
        
        logEventPublisher.publish(logEvent);
    }
    
    /**
     * Logs successful authentication at DEBUG level for audit trail.
     */
    private void logSuccessfulAuthentication(String path, String userId, String email) {
        String message = String.format("[AUTH-SUCCESS] Path: %s | UserId: %s | Email: %s", 
            path, userId, email);
        
        Map<String, Object> context = new HashMap<>();
        context.put("path", path);
        context.put("userId", userId);
        context.put("email", email);
        
        LogEvent logEvent = LogEvent.builder()
                .serviceName("api-gateway")
                .level("DEBUG")
                .eventType("AUTHENTICATION")
                .logger(JwtAuthenticationFilter.class.getName())
                .message(message)
                .timestamp(LocalDateTime.now())
                .threadName(Thread.currentThread().getName())
                .context(context)
                .build();
        
        logEventPublisher.publish(logEvent);
    }
    
    /**
     * Sanitizes token for logging by extracting only non-sensitive metadata.
     * NEVER logs the actual token value or signature.
     */
    private Map<String, Object> sanitizeTokenForLogging(String token, String userId, String email) {
        Map<String, Object> sanitized = new HashMap<>();
        
        if (token != null) {
            // Only log token length and format info, never the actual token
            sanitized.put("tokenLength", token.length());
            sanitized.put("tokenPrefix", token.length() > 10 ? token.substring(0, 10) + "..." : "short");
        }
        
        if (userId != null) {
            sanitized.put("userId", userId);
        }
        
        if (email != null) {
            // Partially mask email for privacy
            sanitized.put("email", maskEmail(email));
        }
        
        return sanitized;
    }
    
    /**
     * Masks email address for privacy (e.g., u***@example.com).
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "invalid";
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***@" + domain;
        }
        
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + domain;
    }
}
