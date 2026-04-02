package com.omnicharge.notification.messaging;

import com.omnicharge.common.event.RechargeCompletedEvent;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.service.IEmailService;
import com.omnicharge.notification.service.INotificationService;
import com.omnicharge.notification.service.ISmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RechargeEventConsumerTest {

    @Mock private IEmailService emailService;
    @Mock private ISmsService smsService;
    @Mock private INotificationService notificationService;

    @InjectMocks
    private RechargeEventConsumer rechargeEventConsumer;

    private RechargeCompletedEvent buildEvent(String status, String email, String mobile) {
        return RechargeCompletedEvent.builder()
                .rechargeId("REC-1").userId(1L).userEmail(email).userMobile(mobile)
                .mobileNumber("9876543210").operatorName("Airtel").planName("Unlimited")
                .amount(new BigDecimal("199")).status(status).transactionId("TXN-1")
                .timestamp(LocalDateTime.now()).build();
    }

    @Test
    void handleRechargeCompleted_Success_SendsEmailAndSms() {
        RechargeCompletedEvent event = buildEvent("SUCCESS", "u@t.com", "9876543210");

        rechargeEventConsumer.handleRechargeCompleted(event);

        verify(emailService).sendRechargeConfirmation("u@t.com", event);
        verify(notificationService).createAndSendEmail(eq(1L), eq("u@t.com"), contains("SUCCESS"),
                anyString(), eq(NotificationCategory.PAYMENT_SUCCESS), eq("REC-1"));
        verify(notificationService).createAndSendSms(eq(1L), eq("9876543210"), anyString(),
                eq(NotificationCategory.PAYMENT_SUCCESS), eq("REC-1"));
    }

    @Test
    void handleRechargeCompleted_Failed_MapsToPaymentFailed() {
        RechargeCompletedEvent event = buildEvent("FAILED", "u@t.com", "9876543210");

        rechargeEventConsumer.handleRechargeCompleted(event);

        verify(notificationService).createAndSendEmail(eq(1L), eq("u@t.com"), contains("FAILED"),
                anyString(), eq(NotificationCategory.PAYMENT_FAILED), eq("REC-1"));
    }

    @Test
    void handleRechargeCompleted_NullEmail_SkipsEmail() {
        RechargeCompletedEvent event = buildEvent("SUCCESS", null, "9876543210");

        rechargeEventConsumer.handleRechargeCompleted(event);

        verify(emailService, never()).sendRechargeConfirmation(anyString(), any());
    }

    @Test
    void handleRechargeCompleted_EmptyMobile_SkipsSms() {
        RechargeCompletedEvent event = buildEvent("SUCCESS", "u@t.com", "");

        rechargeEventConsumer.handleRechargeCompleted(event);

        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void handleRechargeCompleted_EmailThrows_ContinuesToSms() {
        RechargeCompletedEvent event = buildEvent("SUCCESS", "u@t.com", "9876543210");
        doThrow(new RuntimeException("mail error")).when(emailService).sendRechargeConfirmation(anyString(), any());

        rechargeEventConsumer.handleRechargeCompleted(event);

        verify(notificationService).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void handleRechargeCompleted_NullBothContacts_SkipsBoth() {
        RechargeCompletedEvent event = buildEvent("SUCCESS", null, null);

        rechargeEventConsumer.handleRechargeCompleted(event);

        verify(emailService, never()).sendRechargeConfirmation(anyString(), any());
        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }
}
