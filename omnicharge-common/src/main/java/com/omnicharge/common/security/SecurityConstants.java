package com.omnicharge.common.security;

public class SecurityConstants {
    
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    
    public static final String[] PUBLIC_PATHS = {
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/google",
        "/api/auth/refresh-token",
        "/api/auth/forgot-password",
        "/api/auth/verify-otp",
        "/api/auth/reset-password",
        "/actuator/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/swagger-resources/**",
        "/webjars/**"
    };
    
    private SecurityConstants() {
        // Private constructor to prevent instantiation
    }
}
