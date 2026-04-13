package com.omnicharge.notification.messaging;

import com.omnicharge.notification.common.event.PaymentCompletedEvent;
import com.omnicharge.notification.common.logging.LogEventPublisher;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.service.IEmailService;
import com.omnicharge.notification.service.INotificationService;
import com.omnicharge.notification.service.ISmsService;
import org.junit.jupiter.api.DisplayName;
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
class PaymentEventConsumerTest {

    @Mock private IEmailService emailService;
    @Mock private ISmsService smsService;
    @Mock private INotificationService notificationService;
    @Mock private LogEventPublisher logEventPublisher;

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    private PaymentCompletedEvent buildEvent(String status, String email, String mobile) {
        return PaymentCompletedEvent.builder()
                .transactionId("TXN-001")
                .rechargeId("RCH-001")
                .userId(1L)
                .userEmail(email)
                .userMobile(mobile)
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited 28 Days")
                .amount(new BigDecimal("299"))
                .status(status)
                .paymentMethod("UPI")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("SUCCESS event: Sends both email and SMS notifications")
    void handlePaymentCompleted_Success_BothChannels() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", "user@test.com", "+919876543210");

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(emailService, times(1)).sendPaymentConfirmation(eq("user@test.com"), eq(event));
        verify(notificationService, times(1)).createAndSendEmail(
                eq(1L), eq("user@test.com"), anyString(), anyString(),
                eq(NotificationCategory.PAYMENT_SUCCESS), eq("TXN-001"));
        verify(notificationService, times(1)).createAndSendSms(
                eq(1L), eq("+919876543210"), anyString(),
                eq(NotificationCategory.PAYMENT_SUCCESS), eq("TXN-001"));
    }

    @Test
    @DisplayName("FAILED event: Uses PAYMENT_FAILED category")
    void handlePaymentCompleted_Failed_CorrectCategory() {
        PaymentCompletedEvent event = buildEvent("FAILED", "user@test.com", "+919876543210");

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(notificationService, times(1)).createAndSendEmail(
                eq(1L), eq("user@test.com"), anyString(), anyString(),
                eq(NotificationCategory.PAYMENT_FAILED), eq("TXN-001"));
    }

    @Test
    @DisplayName("No email: Skips email notification when email is null")
    void handlePaymentCompleted_NoEmail_SkipsEmail() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", null, "+919876543210");

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(emailService, never()).sendPaymentConfirmation(anyString(), any());
        verify(notificationService, never()).createAndSendEmail(anyLong(), anyString(), anyString(), anyString(), any(), anyString());
        verify(notificationService, times(1)).createAndSendSms(
                eq(1L), eq("+919876543210"), anyString(), any(), eq("TXN-001"));
    }

    @Test
    @DisplayName("No mobile: Skips SMS notification when mobile is null")
    void handlePaymentCompleted_NoMobile_SkipsSms() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", "user@test.com", null);

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
        verify(emailService, times(1)).sendPaymentConfirmation(eq("user@test.com"), eq(event));
    }

    @Test
    @DisplayName("Email failure: Does not crash — SMS still sent")
    void handlePaymentCompleted_EmailFailure_SmsStillSent() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", "user@test.com", "+919876543210");

        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendPaymentConfirmation(anyString(), any());

        paymentEventConsumer.handlePaymentCompleted(event);

        // SMS should still be attempted
        verify(notificationService, times(1)).createAndSendSms(
                eq(1L), eq("+919876543210"), anyString(), any(), eq("TXN-001"));
    }
}
