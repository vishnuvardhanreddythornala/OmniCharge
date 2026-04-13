package com.omnicharge.notification.service;

import com.omnicharge.notification.common.logging.LogEvent;
import com.omnicharge.notification.common.logging.LogEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        // Because the Twilio static call fails (invalid credentials or network blocked),
        // we expect the exception caught and a failure log published natively.
        smsService.sendSms("9876543210", "Test Message");

        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getEventType().equals("SMS_FAILED")));
    }
}
