package com.omnicharge.user.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject variables mimicking @Value annotation behaviour
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "omnicharge-super-secret-key-for-jwt-token-generation-minimum-256-bits");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 1800000L); // 30 minutes
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 604800000L); // 7 days

        jwtUtil.init();
    }

    @Test
    void generateAccessToken_Success() {
        String token = jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateToken_Success() {
        String token = jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true);
        
        Claims claims = jwtUtil.validateToken(token);
        
        assertNotNull(claims);
        assertEquals("test@example.com", claims.getSubject());
        assertEquals("1", claims.get("userId"));
        assertEquals("ROLE_USER", claims.get("role"));
        assertTrue((Boolean) claims.get("isProfileComplete"));
    }

    @Test
    void extractJti_Success() {
        String token = jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true);

        String jti = jwtUtil.extractJti(token);

        assertNotNull(jti);
        assertFalse(jti.isEmpty()); // Should be a valid UUID string automatically mapped by JwtBuilder
    }

    @Test
    void getRemainingExpiration_Success() {
        String token = jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true);

        Long remainingExp = jwtUtil.getRemainingExpiration(token);

        assertNotNull(remainingExp);
        assertTrue(remainingExp > 0L);
        assertTrue(remainingExp <= 1800000L); // Must be slightly less than or equal to 30 mins
    }
}
