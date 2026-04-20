package com.omnicharge.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * CORS and Security Headers Configuration for API Gateway
 * 
 * This configuration:
 * 1. Enables CORS for explicitly allowed origins only (localhost + production domain)
 * 2. Adds Cross-Origin-Opener-Policy header to allow Google OAuth popups
 * 3. Ensures all responses include necessary security headers
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        // Only allow explicitly configured origins — no wildcard
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        corsConfig.setAllowedOrigins(origins);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setExposedHeaders(Arrays.asList("Authorization"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    /**
     * Global filter to add security headers to all responses.
     * 
     * NOTE: Cross-Origin-Opener-Policy header removed to prevent browser warnings
     * when Angular dev server (localhost:4200) makes requests to API Gateway (localhost:8080).
     * The COOP header is not needed for Google Sign-In since we use the embedded button
     * approach (not popup-based OAuth), which doesn't require window.postMessage.
     */
    @Bean
    public WebFilter securityHeadersFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            // COOP header removed - not needed for embedded Google Sign-In button
            // exchange.getResponse().getHeaders().add("Cross-Origin-Opener-Policy", "same-origin-allow-popups");
            exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
            exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
            exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
            return chain.filter(exchange);
        };
    }
}
