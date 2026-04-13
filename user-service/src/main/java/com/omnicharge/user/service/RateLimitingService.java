package com.omnicharge.user.service;

import com.omnicharge.user.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_OTP_SENDS_PER_MOBILE = 3;
    private static final int OTP_SEND_WINDOW_MINUTES = 15;

    private static final int MAX_OTP_SENDS_PER_IP = 10;
    private static final int OTP_SEND_IP_WINDOW_MINUTES = 60;

    private static final int MAX_VERIFY_ATTEMPTS = 3;
    private static final int OTP_VERIFY_WINDOW_MINUTES = 5;

    public void checkAndIncrementSendOtp(String mobileNumber, String ipAddress) {
        // IP Limit Check
        String ipKey = "rate_limit:ip:send_otp:" + ipAddress;
        Long ipCount = redisTemplate.opsForValue().increment(ipKey);
        if (ipCount != null && ipCount == 1) {
            redisTemplate.expire(ipKey, OTP_SEND_IP_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (ipCount != null && ipCount > MAX_OTP_SENDS_PER_IP) {
            log.warn("Rate limit exceeded for IP: {}. Requests: {}", ipAddress, ipCount);
            throw new BadRequestException("Too many OTP requests from this IP address. Please try again later.");
        }

        // Mobile Limit Check
        String mobileKey = "rate_limit:mobile:send_otp:" + mobileNumber;
        Long mobileCount = redisTemplate.opsForValue().increment(mobileKey);
        if (mobileCount != null && mobileCount == 1) {
            redisTemplate.expire(mobileKey, OTP_SEND_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (mobileCount != null && mobileCount > MAX_OTP_SENDS_PER_MOBILE) {
            log.warn("Rate limit exceeded for mobile: {}. Requests: {}", mobileNumber, mobileCount);
            throw new BadRequestException("Too many OTP requests for this mobile number. Please wait 15 minutes.");
        }
    }

    public void checkAndIncrementVerifyAttempts(String mobileNumber) {
        String key = "rate_limit:mobile:verify_otp:" + mobileNumber;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, OTP_VERIFY_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts > MAX_VERIFY_ATTEMPTS) {
            log.warn("Too many OTP verification attempts for mobile: {}", mobileNumber);
            throw new BadRequestException("Too many failed attempts. Please request a new OTP.");
        }
    }

    public void resetVerifyAttempts(String mobileNumber) {
        String key = "rate_limit:mobile:verify_otp:" + mobileNumber;
        redisTemplate.delete(key);
    }
}
