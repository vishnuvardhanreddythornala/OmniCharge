package com.omnicharge.user.service;

import com.omnicharge.user.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void checkAndIncrementSendOtp_SuccessNewIP_NewMobile() {
        when(valueOperations.increment(contains("ip:send_otp"))).thenReturn(1L);
        when(valueOperations.increment(contains("mobile:send_otp"))).thenReturn(1L);
        when(redisTemplate.expire(contains("ip:send_otp"), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisTemplate.expire(contains("mobile:send_otp"), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertDoesNotThrow(() -> rateLimitingService.checkAndIncrementSendOtp("9876543210", "127.0.0.1"));

        verify(redisTemplate, times(2)).expire(anyString(), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    void checkAndIncrementSendOtp_IPLimitExceeded() {
        when(valueOperations.increment(contains("ip:send_otp"))).thenReturn(11L);

        assertThrows(BadRequestException.class, () -> rateLimitingService.checkAndIncrementSendOtp("9876543210", "127.0.0.1"));
        
        // Ensure mobile limit is not even checked if IP limit throws
        verify(valueOperations, never()).increment(contains("mobile:send_otp"));
    }

    @Test
    void checkAndIncrementSendOtp_MobileLimitExceeded() {
        when(valueOperations.increment(contains("ip:send_otp"))).thenReturn(2L);
        when(valueOperations.increment(contains("mobile:send_otp"))).thenReturn(4L);

        assertThrows(BadRequestException.class, () -> rateLimitingService.checkAndIncrementSendOtp("9876543210", "127.0.0.1"));
    }

    @Test
    void checkAndIncrementVerifyAttempts_Success() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        
        assertDoesNotThrow(() -> rateLimitingService.checkAndIncrementVerifyAttempts("9876543210"));
        verify(redisTemplate, times(1)).expire(anyString(), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    void checkAndIncrementVerifyAttempts_LimitExceeded() {
        when(valueOperations.increment(anyString())).thenReturn(4L);
        
        assertThrows(BadRequestException.class, () -> rateLimitingService.checkAndIncrementVerifyAttempts("9876543210"));
    }

    @Test
    void resetVerifyAttempts_Success() {
        when(redisTemplate.delete(anyString())).thenReturn(true);
        assertDoesNotThrow(() -> rateLimitingService.resetVerifyAttempts("9876543210"));
        verify(redisTemplate, times(1)).delete(anyString());
    }
}
