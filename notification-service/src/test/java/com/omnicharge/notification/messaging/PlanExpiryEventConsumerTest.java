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
class PlanExpiryEventConsumerTest {

    @Mock private IEmailService emailService;
    @Mock private ISmsService smsService;
    @Mock private INotificationService notificationService;
    @Mock private LogEventPublisher logEventPublisher;
    @InjectMocks private PlanExpiryEventConsumer consumer;

    private RechargeCompletedEvent buildEvent(String email, String mobile) {
        return RechargeCompletedEvent.builder()
                .rechargeId("REC-123")
                .userId(1L)
                .userEmail(email)
                .userMobile(mobile)
                .mobileNumber("9876543210")
                .operatorName("Jio")
                .planName("Gold")
                .amount(new BigDecimal("199"))
                .status("EXPIRED")
                .build();
    }

    @Test
    void handlePlanExpired_WithEmailAndSms() {
        RechargeCompletedEvent event = buildEvent("user@test.com", "+919876543210");

        consumer.handlePlanExpired(event);

        verify(emailService, times(1)).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(notificationService, times(1)).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void handlePlanExpired_NoEmail() {
        RechargeCompletedEvent event = buildEvent(null, "+919876543210");

        consumer.handlePlanExpired(event);

        verify(emailService, never()).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handlePlanExpired_NoMobile() {
        RechargeCompletedEvent event = buildEvent("user@test.com", null);

        consumer.handlePlanExpired(event);

        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void handlePlanExpired_EmptyEmailAndMobile() {
        RechargeCompletedEvent event = buildEvent("", "");

        consumer.handlePlanExpired(event);

        verify(emailService, never()).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }
}
