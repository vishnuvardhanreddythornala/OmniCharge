package com.omnicharge.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.DuplicateResourceException;
import com.omnicharge.common.exception.UnauthorizedException;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.*;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.RefreshToken;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.RefreshTokenRepository;
import com.omnicharge.user.repository.UserRepository;
import com.omnicharge.user.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPassword("encodedPassword");
        testUser.setMobileNumber("9876543210");
        testUser.setRole(Role.ROLE_USER);
        testUser.setAuthProvider(AuthProvider.LOCAL);
        testUser.setIsActive(true);

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setFullName("Test User");
        registerRequest.setPassword("rawPassword");
        registerRequest.setMobileNumber("9876543210");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("rawPassword");
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByMobileNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        // Mock the saved user
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail(registerRequest.getEmail());
        savedUser.setFullName(registerRequest.getFullName());
        savedUser.setMobileNumber(registerRequest.getMobileNumber());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.register(registerRequest);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void login_InvalidPassword() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
        verify(jwtUtil, never()).generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void login_GoogleUserTriesLocalLogin() {
        testUser.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> authService.login(loginRequest));
    }

    @Test
    void refreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken("valid-refresh-token");
        tokenEntity.setUser(testUser);
        tokenEntity.setExpiryDate(Instant.now().plusSeconds(3600)); // Not expired

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(tokenEntity));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:1")).thenReturn("valid-refresh-token");
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true)).thenReturn("new-access-token");

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
    }

    @Test
    void logout_Success() {
        when(jwtUtil.extractJti("some-jwt-token")).thenReturn("token-jti");
        when(jwtUtil.getRemainingExpiration("some-jwt-token")).thenReturn(1000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout("some-jwt-token");

        verify(valueOperations, times(1)).set("blacklist:token-jti", "true", 1000L, TimeUnit.MILLISECONDS);
    }
}
