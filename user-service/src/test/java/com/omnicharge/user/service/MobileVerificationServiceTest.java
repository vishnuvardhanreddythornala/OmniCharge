package com.omnicharge.user.service;

import com.omnicharge.user.common.exception.BadRequestException;
import com.omnicharge.user.common.exception.DuplicateResourceException;
import com.omnicharge.user.common.exception.ResourceNotFoundException;
import com.omnicharge.user.common.logging.LogEvent;
import com.omnicharge.user.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.AuthResponse;
import com.omnicharge.user.dto.OtpEvent;
import com.omnicharge.user.dto.SendMobileOtpRequest;
import com.omnicharge.user.dto.VerifyMobileOtpRequest;
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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class MobileVerificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private LogEventPublisher logEventPublisher;

    @InjectMocks
    private MobileVerificationService mobileVerificationService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("test@ex.com");
        sampleUser.setIsMobileVerified(false);
        sampleUser.setRole(Role.ROLE_USER);
        sampleUser.setAuthProvider(AuthProvider.LOCAL);
    }

    @Test
    void sendOtp_Success() {
        SendMobileOtpRequest req = new SendMobileOtpRequest();
        req.setMobileNumber("9876543210");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        mobileVerificationService.sendOtp(1L, req);

        verify(valueOperations, times(1)).set(eq("mobile-otp:1"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations, times(1)).set(eq("mobile-otp-num:1"), eq("9876543210"), eq(5L), eq(TimeUnit.MINUTES));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("mobile.otp.send"), any(OtpEvent.class));
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void sendOtp_AlreadyVerified() {
        sampleUser.setIsMobileVerified(true);
        SendMobileOtpRequest req = new SendMobileOtpRequest();
        req.setMobileNumber("9876543210");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThrows(BadRequestException.class, () -> mobileVerificationService.sendOtp(1L, req));
    }

    @Test
    void sendOtp_NumberUsedByAnother() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setIsMobileVerified(true);

        SendMobileOtpRequest req = new SendMobileOtpRequest();
        req.setMobileNumber("9876543210");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(otherUser));

        assertThrows(DuplicateResourceException.class, () -> mobileVerificationService.sendOtp(1L, req));
    }

    @Test
    void verifyOtp_Success() {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("123456");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mobile-otp:1")).thenReturn("123456");
        when(valueOperations.get("mobile-otp-num:1")).thenReturn("9876543210");
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());

        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(anyLong())).thenReturn("refresh_token");

        AuthResponse resp = mobileVerificationService.verifyOtp(1L, req);

        assertNotNull(resp);
        assertEquals("access_token", resp.getAccessToken());
        verify(userRepository, times(1)).save(sampleUser);
        verify(redisTemplate, times(1)).delete("mobile-otp:1");
        verify(redisTemplate, times(1)).delete("mobile-otp-num:1");
    }

    @Test
    void verifyOtp_MismatchNumber() {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("0000000000");
        req.setOtp("123456");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mobile-otp:1")).thenReturn("123456");
        when(valueOperations.get("mobile-otp-num:1")).thenReturn("9876543210");

        assertThrows(BadRequestException.class, () -> mobileVerificationService.verifyOtp(1L, req));
    }

    @Test
    void verifyOtp_InvalidOtp() {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("000000");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mobile-otp:1")).thenReturn("123456");
        when(valueOperations.get("mobile-otp-num:1")).thenReturn("9876543210");

        assertThrows(BadRequestException.class, () -> mobileVerificationService.verifyOtp(1L, req));
    }

    @Test
    void verifyOtp_RaceConditionDuplicateMobile() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setIsMobileVerified(true);

        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("123456");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mobile-otp:1")).thenReturn("123456");
        when(valueOperations.get("mobile-otp-num:1")).thenReturn("9876543210");
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(otherUser));

        assertThrows(DuplicateResourceException.class, () -> mobileVerificationService.verifyOtp(1L, req));
    }
}
