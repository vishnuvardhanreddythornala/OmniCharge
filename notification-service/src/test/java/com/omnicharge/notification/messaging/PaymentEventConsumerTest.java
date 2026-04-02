package com.omnicharge.notification.messaging;

import com.omnicharge.common.event.PaymentCompletedEvent;
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
class PaymentEventConsumerTest {

    @Mock private IEmailService emailService;
    @Mock private ISmsService smsService;
    @Mock private INotificationService notificationService;

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    private PaymentCompletedEvent buildEvent(String status, String email, String mobile) {
        return PaymentCompletedEvent.builder()
                .transactionId("TXN-1").rechargeId("REC-1").userId(1L)
                .userEmail(email).userMobile(mobile).mobileNumber("9876543210")
                .operatorName("Jio").planName("5G Plan").amount(new BigDecimal("299"))
                .status(status).paymentMethod("UPI").timestamp(LocalDateTime.now()).build();
    }

    @Test
    void handlePaymentCompleted_Success_SendsEmailAndSms() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", "u@test.com", "9876543210");

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(emailService).sendPaymentConfirmation("u@test.com", event);
        verify(notificationService).createAndSendEmail(eq(1L), eq("u@test.com"), contains("SUCCESS"),
                anyString(), eq(NotificationCategory.PAYMENT_SUCCESS), eq("TXN-1"));
        verify(notificationService).createAndSendSms(eq(1L), eq("9876543210"), anyString(),
                eq(NotificationCategory.PAYMENT_SUCCESS), eq("TXN-1"));
    }

    @Test
    void handlePaymentCompleted_FailedStatus_MapsToPaymentFailed() {
        PaymentCompletedEvent event = buildEvent("FAILED", "u@test.com", "9876543210");

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(notificationService).createAndSendEmail(eq(1L), eq("u@test.com"), contains("FAILED"),
                anyString(), eq(NotificationCategory.PAYMENT_FAILED), eq("TXN-1"));
    }

    @Test
    void handlePaymentCompleted_NoEmail_SkipsEmailNotification() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", null, "9876543210");

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(emailService, never()).sendPaymentConfirmation(anyString(), any());
        verify(notificationService, never()).createAndSendEmail(anyLong(), anyString(), anyString(),
                anyString(), any(), anyString());
    }

    @Test
    void handlePaymentCompleted_EmptyEmail_SkipsEmailNotification() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", "", "9876543210");

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(emailService, never()).sendPaymentConfirmation(anyString(), any());
    }

    @Test
    void handlePaymentCompleted_NoMobile_SkipsSmsNotification() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", "u@test.com", null);

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(),
                any(), anyString());
    }

    @Test
    void handlePaymentCompleted_EmptyMobile_SkipsSmsNotification() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", "u@test.com", "");

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(),
                any(), anyString());
    }

    @Test
    void handlePaymentCompleted_EmailServiceThrows_ContinuesToSms() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", "u@test.com", "9876543210");
        doThrow(new RuntimeException("SMTP down")).when(emailService).sendPaymentConfirmation(anyString(), any());

        paymentEventConsumer.handlePaymentCompleted(event);

        // SMS should still be attempted
        verify(notificationService).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void handlePaymentCompleted_BothNullEmailAndMobile() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", null, null);

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(emailService, never()).sendPaymentConfirmation(anyString(), any());
        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }
}
