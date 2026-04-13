package com.omnicharge.notification.messaging;

import com.omnicharge.notification.common.event.RechargeCompletedEvent;
import com.omnicharge.notification.common.logging.LogEventPublisher;
import com.omnicharge.notification.service.IEmailService;
import com.omnicharge.notification.service.INotificationService;
import com.omnicharge.notification.service.ISmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RechargeEventConsumerTest {

    @Mock private IEmailService emailService;
    @Mock private ISmsService smsService;
    @Mock private INotificationService notificationService;
    @Mock private LogEventPublisher logEventPublisher;
    @InjectMocks private RechargeEventConsumer consumer;

    private RechargeCompletedEvent buildEvent(String email, String mobile, String status) {
        return RechargeCompletedEvent.builder()
                .rechargeId("REC-123")
                .userId(1L)
                .userEmail(email)
                .userMobile(mobile)
                .mobileNumber("9876543210")
                .operatorName("Jio")
                .planName("Gold")
                .amount(new BigDecimal("199"))
                .status(status)
                .build();
    }

    @Test
    void handleRechargeCompleted_SuccessWithEmailAndSms() {
        RechargeCompletedEvent event = buildEvent("user@test.com", "+919876543210", "SUCCESS");

        consumer.handleRechargeCompleted(event);

        verify(emailService, times(1)).sendRechargeConfirmation(eq("user@test.com"), any());
        verify(notificationService, times(1)).createAndSendEmail(anyLong(), anyString(), anyString(), anyString(), any(), anyString());
        verify(notificationService, times(1)).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void handleRechargeCompleted_FailedStatus() {
        RechargeCompletedEvent event = buildEvent("user@test.com", "+919876543210", "FAILED");

        consumer.handleRechargeCompleted(event);

        verify(emailService, times(1)).sendRechargeConfirmation(eq("user@test.com"), any());
    }

    @Test
    void handleRechargeCompleted_NoEmail() {
        RechargeCompletedEvent event = buildEvent(null, "+919876543210", "SUCCESS");

        consumer.handleRechargeCompleted(event);

        verify(emailService, never()).sendRechargeConfirmation(anyString(), any());
    }

    @Test
    void handleRechargeCompleted_NoMobile() {
        RechargeCompletedEvent event = buildEvent("user@test.com", null, "SUCCESS");

        consumer.handleRechargeCompleted(event);

        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void handleRechargeCompleted_EmptyEmail() {
        RechargeCompletedEvent event = buildEvent("", "+919876543210", "SUCCESS");

        consumer.handleRechargeCompleted(event);

        verify(emailService, never()).sendRechargeConfirmation(anyString(), any());
    }
}
