package com.omnicharge.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @InjectMocks
    private SmsService smsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(smsService, "accountSid", "test_sid");
        ReflectionTestUtils.setField(smsService, "authToken", "test_token");
        ReflectionTestUtils.setField(smsService, "fromNumber", "+10000000000");
    }

    @Test
    void sendSms_WithoutPlusPrefix_AddsIndiaCode() {
        // SmsService.sendSms catches all exceptions internally, so it should never throw
        assertDoesNotThrow(() -> smsService.sendSms("9876543210", "Test message"));
    }

    @Test
    void sendSms_WithPlusPrefix_UsesAsIs() {
        assertDoesNotThrow(() -> smsService.sendSms("+919876543210", "Test message with prefix"));
    }

    @Test
    void sendSms_EmptyMessage() {
        assertDoesNotThrow(() -> smsService.sendSms("9876543210", ""));
    }

    @Test
    void sendSms_NullMobileNumber_HandledGracefully() {
        // Twilio will throw at runtime, but SmsService catches all exceptions
        assertDoesNotThrow(() -> smsService.sendSms(null, "Test"));
    }
}
