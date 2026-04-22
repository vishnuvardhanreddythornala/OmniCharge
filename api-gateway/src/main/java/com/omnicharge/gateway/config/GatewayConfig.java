package com.omnicharge.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    /*
     * Redis-based rate limiter
     * replenishRate: Average requests per second allowed
     * burstCapacity: Maximum requests allowed in a single second (burst)
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 100 requests per minute = ~1.67 requests per second
        // Burst capacity: 150 requests
        return new RedisRateLimiter(2, 3, 1);
    }

    /*
     * Configure routes with rate limiting
     * All routes through Eureka discovery will have rate limiting applied
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, 
                                           RedisRateLimiter rateLimiter,
                                           KeyResolver keyResolver) {
        return builder.routes()
                // User Service routes with rate limiting
                .route("user-service", r -> r
                        .path("/api/auth/**", "/api/users/**", "/api/admin/users/**", "/api/admin/dashboard/**", "/v3/api-docs/user-service")
                        .filters(f -> f
                                .rewritePath("/v3/api-docs/user-service", "/v3/api-docs")
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(rateLimiter)
                                        .setKeyResolver(keyResolver)
                                        .setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)))
                        .uri("lb://user-service"))
                
                // Payment Service routes with rate limiting
                .route("payment-service", r -> r
                        .path("/api/payments/**", "/api/admin/payments/**", "/v3/api-docs/payment-service")
                        .filters(f -> f
                                .rewritePath("/v3/api-docs/payment-service", "/v3/api-docs")
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(rateLimiter)
                                        .setKeyResolver(keyResolver)
                                        .setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)))
                        .uri("lb://payment-service"))
                
                // Operator Service routes with rate limiting
                .route("operator-service", r -> r
                        .path("/api/operators/**", "/api/plans/**", "/api/admin/operators/**", "/api/admin/system/**", "/v3/api-docs/operator-service")
                        .filters(f -> f
                                .rewritePath("/v3/api-docs/operator-service", "/v3/api-docs")
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(rateLimiter)
                                        .setKeyResolver(keyResolver)
                                        .setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)))
                        .uri("lb://operator-service"))
                
                // Recharge Service routes with rate limiting
                .route("recharge-service", r -> r
                        .path("/api/recharges/**", "/api/admin/recharges/**", "/v3/api-docs/recharge-service")
                        .filters(f -> f
                                .rewritePath("/v3/api-docs/recharge-service", "/v3/api-docs")
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(rateLimiter)
                                        .setKeyResolver(keyResolver)
                                        .setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)))
                        .uri("lb://recharge-service"))
                
                // Notification Service routes with rate limiting
                .route("notification-service", r -> r
                        .path("/api/notifications/**", "/api/admin/notifications/**", "/v3/api-docs/notification-service")
                        .filters(f -> f
                                .rewritePath("/v3/api-docs/notification-service", "/v3/api-docs")
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(rateLimiter)
                                        .setKeyResolver(keyResolver)
                                        .setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)))
                        .uri("lb://notification-service"))
                
                .build();
    }
}
