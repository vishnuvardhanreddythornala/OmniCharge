package com.omnicharge.gateway.filter;

import com.omnicharge.gateway.common.logging.LogEvent;
import com.omnicharge.gateway.common.logging.LogEventPublisher;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String secret = "12345678901234567890123456789012abcdefGHIJKL"; // 256 bits

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "jwtSecret", secret);
    }

    @Test
    void filter_PublicPath_AllowsWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/login").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        jwtAuthenticationFilter.filter(exchange, filterChain).block();

        verify(filterChain, times(1)).filter(exchange);
        verifyNoInteractions(reactiveRedisTemplate);
    }

    @Test
    void filter_MissingToken_RejectsUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/secure/data").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthenticationFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void filter_ValidToken_Allows() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .claim("userId", "1")
                .claim("email", "test@test.com")
                .claim("role", "ROLE_USER")
                .claim("isProfileComplete", true)
                .claim("jti", "jti-123")
                .expiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/secure/data")
                .header("Authorization", "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(reactiveRedisTemplate.hasKey("blacklist:jti-123")).thenReturn(Mono.just(false));
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        jwtAuthenticationFilter.filter(exchange, filterChain).block();

        verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getLevel().equals("DEBUG")));
    }

    @Test
    void filter_BlacklistedToken_RejectsUnauthorized() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .claim("userId", "1")
                .claim("email", "test@test.com")
                .claim("isProfileComplete", true)
                .claim("jti", "jti-123")
                .expiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/secure/data")
                .header("Authorization", "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(reactiveRedisTemplate.hasKey("blacklist:jti-123")).thenReturn(Mono.just(true));

        jwtAuthenticationFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getLevel().equals("WARN")));
    }

    @Test
    void filter_IncompleteProfile_RejectsForbidden() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .claim("userId", "1")
                .claim("isProfileComplete", false)
                .expiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/recharges/process")
                .header("Authorization", "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthenticationFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_InvalidTokenSignature_RejectsUnauthorized() {
        // Build token with different secret key
        SecretKey otherKey = Keys.hmacShaKeyFor("differentFakeKeyOf256BitsMinimum123".getBytes(StandardCharsets.UTF_8));
        String weakToken = Jwts.builder()
                .claim("userId", "1")
                .signWith(otherKey)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/secure/data")
                .header("Authorization", "Bearer " + weakToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthenticationFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getLevel().equals("WARN")));
    }
}
