package com.omnicharge.notification.service;

import com.omnicharge.notification.common.logging.LogEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private SmsService smsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(smsService, "accountSid", "invalid_sid");
        ReflectionTestUtils.setField(smsService, "authToken", "invalid_token");
        ReflectionTestUtils.setField(smsService, "fromNumber", "+1234567890");
    }

    @Test
    void sendSms_FailureTriggersBusinessLogWithoutCrashing() {
        // Twilio.init() with invalid credentials, Message.creator() will fail
        smsService.sendSms("9876543210", "Test Message");

        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getEventType().equals("SMS_FAILED")));
    }

    @Test
    void sendSms_WithInternationalPrefix_FailureHandled() {
        // Number already starts with +, should proceed directly
        smsService.sendSms("+919876543210", "Test Message With Prefix");

        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getEventType().equals("SMS_FAILED")));
    }

    @Test
    void sendSms_EmptyMessage_FailureHandled() {
        smsService.sendSms("9876543210", "");

        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getEventType().equals("SMS_FAILED")));
    }

    @Test
    void sendSms_LogPublisherException_DoesNotThrow() {
        doThrow(new RuntimeException("Log publish failed")).when(logEventPublisher).publish(any());

        // Even if log publisher fails, sendSms should handle it gracefully
        smsService.sendSms("9876543210", "Test Message");
    }
}
