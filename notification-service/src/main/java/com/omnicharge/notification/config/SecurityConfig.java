package com.omnicharge.notification.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration for Notification Service
 * 
 * This service trusts the API Gateway for authentication.
 * The Gateway validates JWT tokens and adds user info to request headers:
 * - X-User-Id: User's ID
 * - X-User-Role: User's role (ROLE_USER, ROLE_ADMIN)
 * - X-User-Email: User's email
 * 
 * This configuration:
 * 1. Disables CSRF (not needed for stateless REST APIs)
 * 2. Extracts authentication from Gateway headers via GatewayAuthenticationFilter
 * 3. Uses stateless session management
 * 4. Allows actuator endpoints for health checks
 * 5. Enables method-level security (@PreAuthorize) for admin endpoints
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST API
            .csrf(csrf -> csrf.disable())
            
            // Stateless session management (no session cookies)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure authorization
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - actuator for health checks
                .requestMatchers("/actuator/**").permitAll()
                
                // Test endpoints - allow without authentication for testing
                .requestMatchers("/api/test/**").permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Add filter to extract authentication from gateway headers
            .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
