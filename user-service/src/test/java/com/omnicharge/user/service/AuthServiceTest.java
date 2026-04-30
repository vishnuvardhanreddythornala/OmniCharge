package com.omnicharge.user.service;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.omnicharge.user.common.exception.BadRequestException;
import com.omnicharge.user.common.exception.ForbiddenException;
import com.omnicharge.user.common.exception.UnauthorizedException;
import com.omnicharge.user.common.logging.LogEvent;
import com.omnicharge.user.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.*;
import com.omnicharge.user.entity.AuthProvider;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private GoogleIdTokenVerifier googleIdTokenVerifier;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private LogEventPublisher logEventPublisher;
    @Mock private RateLimitingService rateLimitingService;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AuthService authService;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("encoded_pass");
        adminUser.setRole(Role.ROLE_ADMIN);
        adminUser.setIsActive(true);
        adminUser.setAuthProvider(AuthProvider.LOCAL);

        normalUser = new User();
        normalUser.setId(2L);
        normalUser.setEmail("user@test.com");
        normalUser.setRole(Role.ROLE_USER);
        normalUser.setIsActive(true);
        normalUser.setAuthProvider(AuthProvider.LOCAL);
    }

    @Test
    void login_AdminSuccess() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("pass");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("pass", "encoded_pass")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AdminLoginInitResponse resp = authService.login(req, "127.0.0.1");

        assertTrue(resp.isRequires2fa());
        assertEquals("admin@test.com", resp.getEmail());
        verify(valueOperations, times(1)).set(eq("admin:2fa:otp:admin@test.com"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("email.otp.send"), any(OtpEvent.class));
    }

    @Test
    void login_NormalUserForbidden() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("pass");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(normalUser));

        assertThrows(ForbiddenException.class, () -> authService.login(req, "127.0.0.1"));
    }

    @Test
    void login_BadPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("badpass");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("badpass", "encoded_pass")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(req, "127.0.0.1"));
    }
    
    @Test
    void login_DisabledAccount() {
        adminUser.setIsActive(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("pass");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("pass", "encoded_pass")).thenReturn(true);

        assertThrows(UnauthorizedException.class, () -> authService.login(req, "127.0.0.1"));    
    }

    @Test
    void verifyAdmin2fa_Success() {
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("admin@test.com");
        req.setOtp("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("admin:2fa:otp:admin@test.com")).thenReturn("123456");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(anyLong())).thenReturn("refresh_token");

        AuthResponse resp = authService.verifyAdmin2fa(req, "127.0.0.1");

        assertEquals("access_token", resp.getAccessToken());
        verify(redisTemplate, times(1)).delete("admin:2fa:otp:admin@test.com");
    }

    @Test
    void verifyAdmin2fa_BadOtp() {
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("admin@test.com");
        req.setOtp("111111");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("admin:2fa:otp:admin@test.com")).thenReturn("123456");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        assertThrows(BadRequestException.class, () -> authService.verifyAdmin2fa(req, "127.0.0.1"));
    }

    @Test
    void sendEmailOtp_Success() throws Exception {
        SendEmailOtpRequest req = new SendEmailOtpRequest();
        req.setEmail("user@test.com");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(normalUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.sendEmailOtp(req, "127.0.0.1");

        verify(rateLimitingService, times(1)).checkAndIncrementSendOtp("user@test.com", "127.0.0.1");
        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("email.otp.send"), any(OtpEvent.class));
    }

    @Test
    void sendEmailOtp_AdminForbidden()  {
        SendEmailOtpRequest req = new SendEmailOtpRequest();
        req.setEmail("admin@test.com");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        assertThrows(ForbiddenException.class, () -> authService.sendEmailOtp(req, "127.0.0.1"));
    }

    @Test
    void verifyEmailOtp_Success()  {
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("user@test.com");
        req.setOtp("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:email:login:otp:user@test.com")).thenReturn("123456");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(normalUser));
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString())).thenReturn("access_token");

        AuthResponse resp = authService.verifyEmailOtp(req);

        assertEquals("access_token", resp.getAccessToken());
        verify(rateLimitingService, times(1)).resetVerifyAttempts("user@test.com");
    }

    @Test
    void authenticateWithGoogle_Success() throws Exception {
        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setIdToken("valid_token");

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload mockPayload = new GoogleIdToken.Payload();
        mockPayload.setSubject("g_123");
        mockPayload.setEmail("user@test.com");
        mockPayload.set("name", "Test User");

        when(googleIdTokenVerifier.verify("valid_token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(userRepository.findByGoogleId("g_123")).thenReturn(Optional.of(normalUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString())).thenReturn("access_token");

        AuthResponse resp = authService.authenticateWithGoogle(req, "127.0.0.1");

        assertNotNull(resp);
        assertEquals("access_token", resp.getAccessToken());
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void verifyEmailOtp_NewUserProvisioning() {
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("new@test.com");
        req.setOtp("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:email:login:otp:new@test.com")).thenReturn("123456");
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        
        User savedUser = new User();
        savedUser.setId(3L);
        savedUser.setEmail("new@test.com");
        savedUser.setRole(Role.ROLE_USER);
        savedUser.setIsActive(true);
        savedUser.setAuthProvider(AuthProvider.LOCAL);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        AuthResponse resp = authService.verifyEmailOtp(req);
        assertNotNull(resp);
    }

    @Test
    void verifyMobileOtp_Success()  {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:mobile:login:otp:9876543210")).thenReturn("123456");
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(normalUser));

        AuthResponse resp = authService.verifyMobileOtp(req);
        assertNotNull(resp);
    }

    @Test
    void sendMobileOtp_Success() throws Exception {
        SendMobileOtpRequest req = new SendMobileOtpRequest();
        req.setMobileNumber("9876543210");

        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.sendMobileOtp(req, "127.0.0.1");

        verify(rateLimitingService, times(1)).checkAndIncrementSendOtp("9876543210", "127.0.0.1");
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(OtpEvent.class));
    }

    @Test
    void authenticateWithGoogle_InvalidToken() throws Exception {
        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setIdToken("invalid");

        when(googleIdTokenVerifier.verify("invalid")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> authService.authenticateWithGoogle(req, "127.0.0.1"));
    }

    @Test
    void refreshToken_Success() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("token_val");

        com.omnicharge.user.entity.RefreshToken rt = new com.omnicharge.user.entity.RefreshToken();
        rt.setToken("token_val");
        rt.setExpiryDate(Instant.now().plusSeconds(600));
        rt.setUser(normalUser);

        when(refreshTokenRepository.findByToken("token_val")).thenReturn(Optional.of(rt));
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString()))
                .thenReturn("new_access");

        AuthResponse resp = authService.refreshToken(req);
        assertEquals("new_access", resp.getAccessToken());
    }

    @Test
    void refreshToken_Expired() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("token_val");

        com.omnicharge.user.entity.RefreshToken rt = new com.omnicharge.user.entity.RefreshToken();
        rt.setToken("token_val");
        rt.setExpiryDate(Instant.now().minusSeconds(600));

        when(refreshTokenRepository.findByToken("token_val")).thenReturn(Optional.of(rt));

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(req));
        verify(refreshTokenRepository, times(1)).delete(rt);
    }

    @Test
    void logout_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.extractJti("myToken")).thenReturn("jti123");
        when(jwtUtil.getRemainingExpiration("myToken")).thenReturn(100L);

        authService.logout("myToken");

        verify(valueOperations, times(1)).set("blacklist:jti123", "true", 100L, TimeUnit.MILLISECONDS);
    }
    
    @Test
    void logoutByRefreshToken_Success() {
        com.omnicharge.user.entity.RefreshToken rt = new com.omnicharge.user.entity.RefreshToken();
        rt.setToken("rt_val");
        rt.setUser(normalUser);
        
        when(refreshTokenRepository.findByToken("rt_val")).thenReturn(Optional.of(rt));
        
        authService.logoutByRefreshToken("rt_val");
        
        verify(redisTemplate, times(1)).delete("refresh:" + normalUser.getId() + ":rt_val");
        verify(refreshTokenRepository, times(1)).delete(rt);
    }

    @Test
    void generateAuthResponse_MaxDevicesExceeded() {
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("user@test.com");
        req.setOtp("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:email:login:otp:user@test.com")).thenReturn("123456");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(normalUser));
        
        // Mock that there are already 5 devices
        when(refreshTokenRepository.countByUser(normalUser)).thenReturn(5L);
        
        com.omnicharge.user.entity.RefreshToken oldRt1 = new com.omnicharge.user.entity.RefreshToken();
        oldRt1.setToken("old1");
        com.omnicharge.user.entity.RefreshToken oldRt2 = new com.omnicharge.user.entity.RefreshToken();
        oldRt2.setToken("old2");

        when(refreshTokenRepository.findByUserOrderByExpiryDateAsc(normalUser)).thenReturn(java.util.List.of(oldRt1, oldRt2));

        AuthResponse resp = authService.verifyEmailOtp(req);

        assertNotNull(resp);
        verify(refreshTokenRepository, atLeastOnce()).delete(any(com.omnicharge.user.entity.RefreshToken.class));
    }

    @Test
    void sendMobileOtp_AdminForbidden() {
        SendMobileOtpRequest req = new SendMobileOtpRequest();
        req.setMobileNumber("9876543210");

        adminUser.setMobileNumber("9876543210");
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(adminUser));

        assertThrows(ForbiddenException.class, () -> authService.sendMobileOtp(req, "127.0.0.1"));
    }

    @Test
    void verifyMobileOtp_NewUserProvisioning() {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("9999999999");
        req.setOtp("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:mobile:login:otp:9999999999")).thenReturn("123456");
        when(userRepository.findByMobileNumber("9999999999")).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setMobileNumber("9999999999");
        savedUser.setRole(Role.ROLE_USER);
        savedUser.setIsActive(true);
        savedUser.setIsMobileVerified(true);
        savedUser.setAuthProvider(AuthProvider.MOBILE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse resp = authService.verifyMobileOtp(req);
        assertNotNull(resp);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void verifyMobileOtp_AdminForbidden() {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("123456");

        adminUser.setMobileNumber("9876543210");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:mobile:login:otp:9876543210")).thenReturn("123456");
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(adminUser));

        assertThrows(ForbiddenException.class, () -> authService.verifyMobileOtp(req));
    }

    @Test
    void verifyMobileOtp_DisabledAccount() {
        normalUser.setIsActive(false);
        normalUser.setMobileNumber("9876543210");
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:mobile:login:otp:9876543210")).thenReturn("123456");
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(normalUser));

        assertThrows(UnauthorizedException.class, () -> authService.verifyMobileOtp(req));
    }

    @Test
    void verifyMobileOtp_BadOtp() {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("111111");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:mobile:login:otp:9876543210")).thenReturn("123456");

        assertThrows(BadRequestException.class, () -> authService.verifyMobileOtp(req));
    }

    @Test
    void verifyEmailOtp_BadOtp() {
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("user@test.com");
        req.setOtp("999999");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:email:login:otp:user@test.com")).thenReturn("123456");

        assertThrows(BadRequestException.class, () -> authService.verifyEmailOtp(req));
    }

    @Test
    void verifyEmailOtp_AdminForbidden() {
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("admin@test.com");
        req.setOtp("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:email:login:otp:admin@test.com")).thenReturn("123456");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        assertThrows(ForbiddenException.class, () -> authService.verifyEmailOtp(req));
    }

    @Test
    void verifyEmailOtp_DisabledAccount() {
        normalUser.setIsActive(false);
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("user@test.com");
        req.setOtp("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("public:email:login:otp:user@test.com")).thenReturn("123456");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(normalUser));

        assertThrows(UnauthorizedException.class, () -> authService.verifyEmailOtp(req));
    }

    @Test
    void verifyAdmin2fa_ExpiredOtp() {
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("admin@test.com");
        req.setOtp("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("admin:2fa:otp:admin@test.com")).thenReturn(null);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        assertThrows(BadRequestException.class, () -> authService.verifyAdmin2fa(req, "127.0.0.1"));
    }

    @Test
    void authenticateWithGoogle_NewUserProvisioning() throws Exception {
        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setIdToken("valid_token");

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload mockPayload = new GoogleIdToken.Payload();
        mockPayload.setSubject("g_new");
        mockPayload.setEmail("new_google@test.com");
        mockPayload.set("name", "New Google User");

        when(googleIdTokenVerifier.verify("valid_token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(userRepository.findByGoogleId("g_new")).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setId(20L);
        savedUser.setEmail("new_google@test.com");
        savedUser.setRole(Role.ROLE_USER);
        savedUser.setIsActive(true);
        savedUser.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString())).thenReturn("access_token");

        AuthResponse resp = authService.authenticateWithGoogle(req, "127.0.0.1");

        assertNotNull(resp);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void authenticateWithGoogle_DisabledAccount() throws Exception {
        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setIdToken("valid_token");

        normalUser.setIsActive(false);

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload mockPayload = new GoogleIdToken.Payload();
        mockPayload.setSubject("g_123");
        mockPayload.setEmail("user@test.com");
        mockPayload.set("name", "Test User");

        when(googleIdTokenVerifier.verify("valid_token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(userRepository.findByGoogleId("g_123")).thenReturn(Optional.of(normalUser));

        assertThrows(UnauthorizedException.class, () -> authService.authenticateWithGoogle(req, "127.0.0.1"));
    }

    @Test
    void logout_ExceptionHandledGracefully() {
        when(jwtUtil.extractJti("badToken")).thenThrow(new RuntimeException("Parse failure"));

        // Should not throw
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> authService.logout("badToken"));
    }

    @Test
    void logoutByRefreshToken_NotFound() {
        when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        // Should not throw
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> authService.logoutByRefreshToken("nonexistent"));
    }

    @Test
    void login_UserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("notfound@test.com");
        req.setPassword("pass");

        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(req, "127.0.0.1"));
    }

    @Test
    void refreshToken_NotFound() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("invalid_token");

        when(refreshTokenRepository.findByToken("invalid_token")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(req));
    }

    @Test
    void sendEmailOtp_NoExistingUser() {
        SendEmailOtpRequest req = new SendEmailOtpRequest();
        req.setEmail("newuser@test.com");

        when(userRepository.findByEmail("newuser@test.com")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.sendEmailOtp(req, "127.0.0.1");

        verify(rateLimitingService, times(1)).checkAndIncrementSendOtp("newuser@test.com", "127.0.0.1");
        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("email.otp.send"), any(OtpEvent.class));
    }
}
