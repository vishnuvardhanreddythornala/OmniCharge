package com.omnicharge.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RateLimitConfigTest {

    private final RateLimitConfig rateLimitConfig = new RateLimitConfig();

    @Test
    void keyResolver_WithUserIdHeader_ResolvesToUserId() {
        KeyResolver resolver = rateLimitConfig.userKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/recharges/history")
                .header("X-User-Id", "42").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("42")
                .verifyComplete();
    }

    @Test
    void keyResolver_EmptyUserIdHeader_FallsBackToIp() {
        KeyResolver resolver = rateLimitConfig.userKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/payments/process")
                .header("X-User-Id", "").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .assertNext(key -> assertNotEquals("", key)) // Should be IP, not empty
                .verifyComplete();
    }

    @Test
    void keyResolver_NoUserIdHeader_FallsBackToIp() {
        KeyResolver resolver = rateLimitConfig.userKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .assertNext(key -> assertNotNull(key))
                .verifyComplete();
    }

    @Test
    void keyResolver_MultipleUserId_UsesFirst() {
        KeyResolver resolver = rateLimitConfig.userKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header("X-User-Id", "10")
                .header("X-User-Id", "20").build();  // Multiple header values
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("10")
                .verifyComplete();
    }
}
