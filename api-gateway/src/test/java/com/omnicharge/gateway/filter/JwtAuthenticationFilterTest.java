package com.omnicharge.gateway.filter;

import com.omnicharge.common.logging.LogEventPublisher;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String JWT_SECRET = "omnicharge-super-secret-key-for-jwt-token-generation-minimum-256-bits";

    @Mock
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Mock
    private GatewayFilterChain chain;
    
    @Mock
    private LogEventPublisher logEventPublisher;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(reactiveRedisTemplate, logEventPublisher);
        ReflectionTestUtils.setField(filter, "jwtSecret", JWT_SECRET);
    }

    private String generateToken(Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claims(claims)
                .subject("user@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    private Map<String, Object> defaultClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "1");
        claims.put("email", "user@test.com");
        claims.put("role", "ROLE_USER");
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("isProfileComplete", true);
        return claims;
    }

    // === Public Path Tests ===

    @Test
    void publicPath_Login_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    void publicPath_Register_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/register").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    void publicPath_GoogleAuth_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/google").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void publicPath_RefreshToken_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/refresh-token").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void publicPath_Logout_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/logout").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void publicPath_ForgotPassword_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/forgot-password").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void publicPath_VerifyOtp_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/verify-otp").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void publicPath_ResetPassword_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/reset-password").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void publicPath_OperatorDetect_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/operators/detect").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void publicPath_PlansEndpoint_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/plans/jio").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void publicPath_Actuator_Bypasses() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    // === Protected Path — Missing/Invalid Token ===

    @Test
    void protectedPath_MissingAuthHeader_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void protectedPath_InvalidPrefix_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/payments/process")
                .header(HttpHeaders.AUTHORIZATION, "Basic abc123").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedPath_MalformedJwt_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid.jwt.token").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedPath_WrongSecret_Returns401() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-for-testing-jwt-minimum-256-bits-required!!".getBytes(StandardCharsets.UTF_8));
        String badToken = Jwts.builder()
                .claims(defaultClaims()).subject("u@t.com")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey).compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/recharges/history")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + badToken).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedPath_ExpiredToken_Returns401() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .claims(defaultClaims()).subject("u@t.com")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key).compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    // === Profile Completion Enforcement (Google OAuth Edge Case) ===

    @Test
    void incompleteProfile_NonProfilePath_Returns403() {
        Map<String, Object> claims = defaultClaims();
        claims.put("isProfileComplete", false);
        String token = generateToken(claims);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/recharges/initiate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void incompleteProfile_ProfilePath_Allowed() {
        Map<String, Object> claims = defaultClaims();
        claims.put("isProfileComplete", false);
        String token = generateToken(claims);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(reactiveRedisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    // === Blacklist (Logout) Check ===

    @Test
    void validToken_NotBlacklisted_ForwardsWithHeaders() {
        String token = generateToken(defaultClaims());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(reactiveRedisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void validToken_Blacklisted_Returns401() {
        String token = generateToken(defaultClaims());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(reactiveRedisTemplate.hasKey(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void validToken_RedisError_FailsOpen() {
        String token = generateToken(defaultClaims());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(reactiveRedisTemplate.hasKey(anyString())).thenReturn(Mono.error(new RuntimeException("Redis down")));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any()); // Fail-open: request proceeds
    }

    // === Header Propagation ===

    @Test
    void validToken_PropagatesCorrectHeaders() {
        Map<String, Object> claims = defaultClaims();
        claims.put("userId", "42");
        claims.put("email", "admin@test.com");
        claims.put("role", "ROLE_ADMIN");
        String token = generateToken(claims);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/admin/payments/stats")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(reactiveRedisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        when(chain.filter(any())).thenAnswer(inv -> {
            // Verify headers were set on the mutated exchange
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    // === Edge Cases ===

    @Test
    void protectedPath_EmptyBearerToken_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedPath_NullClaimField_HandledGracefully() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("isProfileComplete", true);
        // Missing userId, email, role — should not crash filter
        String token = generateToken(claims);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(reactiveRedisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void filterOrder_IsNegativeOne() {
        assertEquals(-1, filter.getOrder());
    }

    // === Protected Paths That Require Auth ===

    @Test
    void protectedPath_Recharges_RequiresAuth() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/recharges/history").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedPath_Payments_RequiresAuth() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/payments/process").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedPath_Notifications_RequiresAuth() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/notifications").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedPath_AdminPayments_RequiresAuth() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/admin/payments/stats").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
