package com.omnicharge.common.security;

public class JwtConstants {
    
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_TOKEN_PREFIX = "Bearer ";
    public static final String JWT_CLAIM_USER_ID = "userId";
    public static final String JWT_CLAIM_ROLE = "role";
    public static final String JWT_CLAIM_EMAIL = "email";
    
    private JwtConstants() {
        // Private constructor to prevent instantiation
    }
}
