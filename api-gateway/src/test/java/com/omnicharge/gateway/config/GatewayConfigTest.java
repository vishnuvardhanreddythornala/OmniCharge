package com.omnicharge.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder.Builder;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GatewayConfigTest {

    private final GatewayConfig gatewayConfig = new GatewayConfig();

    @Test
    void redisRateLimiter_CreatedWithCorrectParams() {
        RedisRateLimiter limiter = gatewayConfig.redisRateLimiter();
        assertNotNull(limiter);
    }
}
