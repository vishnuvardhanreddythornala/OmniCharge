package com.omnicharge.notification.messaging;

import com.omnicharge.notification.dto.OtpEvent;
import com.omnicharge.notification.service.ISmsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpEventConsumerTest {

    @Mock private ISmsService smsService;
    @Mock private JavaMailSender emailSender;
    @Mock private MimeMessage mimeMessage;
    @Mock
    private com.omnicharge.notification.common.logging.LogEventPublisher logEventPublisher;


    @InjectMocks
    private OtpEventConsumer otpEventConsumer;

    @Test
    @DisplayName("SMS OTP: Routes to SMS service for mobile numbers")
    void consumeOtpEvent_SmsOtp() {
        OtpEvent event = new OtpEvent();
        event.setMobileNumber("+919876543210");
        event.setOtp("123456");

        otpEventConsumer.consumeOtpEvent(event);

        verify(smsService, times(1)).sendSms(eq("+919876543210"), contains("123456"));
    }

    @Test
    @DisplayName("Email OTP: Routes to email service for email addresses")
    void consumeOtpEvent_EmailOtp() throws Exception {
        OtpEvent event = new OtpEvent();
        event.setMobileNumber("user@test.com");
        event.setOtp("654321");

        when(emailSender.createMimeMessage()).thenReturn(mimeMessage);

        otpEventConsumer.consumeOtpEvent(event);

        verify(emailSender, times(1)).send(any(MimeMessage.class));
        verify(smsService, never()).sendSms(anyString(), anyString());
    }

    @Test
    @DisplayName("SMS failure: Does not crash consumer")
    void consumeOtpEvent_SmsFailure_NoException() {
        OtpEvent event = new OtpEvent();
        event.setMobileNumber("+919876543210");
        event.setOtp("123456");

        doThrow(new RuntimeException("Twilio down")).when(smsService).sendSms(anyString(), anyString());

        // Should not throw
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            otpEventConsumer.consumeOtpEvent(event);
        });
    }
}
