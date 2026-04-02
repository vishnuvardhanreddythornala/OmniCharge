package com.omnicharge.notification.service;

import com.omnicharge.common.event.PaymentCompletedEvent;
import com.omnicharge.common.event.RechargeCompletedEvent;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@omnicharge.com");
        ReflectionTestUtils.setField(emailService, "mailPassword", "testpassword");
    }

    // === sendPaymentConfirmation Tests ===

    @Test
    void sendPaymentConfirmation_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("TXN-1").rechargeId("REC-1").userId(1L).userEmail("u@t.com")
                .mobileNumber("9876543210").operatorName("Jio").planName("5G Plan")
                .amount(new BigDecimal("299")).status("SUCCESS").paymentMethod("UPI")
                .timestamp(LocalDateTime.now()).build();

        assertDoesNotThrow(() -> emailService.sendPaymentConfirmation("u@t.com", event));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPaymentConfirmation_FailedStatus() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("TXN-2").rechargeId("REC-2").userId(1L).userEmail("u@t.com")
                .mobileNumber("9876").operatorName("Airtel").planName("Plan A")
                .amount(new BigDecimal("199")).status("FAILED").paymentMethod("CREDIT_CARD")
                .timestamp(LocalDateTime.now()).build();

        assertDoesNotThrow(() -> emailService.sendPaymentConfirmation("u@t.com", event));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPaymentConfirmation_MailSenderThrows_WrapsInRuntime() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("TXN-3").status("SUCCESS").timestamp(LocalDateTime.now()).build();

        assertThrows(RuntimeException.class, () -> emailService.sendPaymentConfirmation("bad@t.com", event));
    }

    // === sendRechargeConfirmation Tests ===

    @Test
    void sendRechargeConfirmation_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                .rechargeId("REC-1").userId(1L).mobileNumber("9876543210")
                .operatorName("Vi").planName("Basic").amount(new BigDecimal("99"))
                .status("SUCCESS").transactionId("TXN-1").timestamp(LocalDateTime.now()).build();

        assertDoesNotThrow(() -> emailService.sendRechargeConfirmation("u@t.com", event));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendRechargeConfirmation_FailedRecharge() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                .rechargeId("REC-2").status("FAILED").timestamp(LocalDateTime.now())
                .operatorName("BSNL").planName("PlanX").amount(new BigDecimal("50"))
                .mobileNumber("1234").transactionId("TXN-F").build();

        assertDoesNotThrow(() -> emailService.sendRechargeConfirmation("u@t.com", event));
    }

    // === sendPlanExpiryReminder Tests ===

    @Test
    void sendPlanExpiryReminder_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendPlanExpiryReminder("u@t.com", "User", "Jio", "5G", "9876543210", 5));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPlanExpiryReminder_OneDayLeft() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendPlanExpiryReminder("u@t.com", "User", "Airtel", "Plan", "123", 1));
    }

    @Test
    void sendPlanExpiryReminder_MailFailure() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("Timeout")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class, () ->
                emailService.sendPlanExpiryReminder("u@t.com", "User", "Jio", "Plan", "123", 3));
    }

    // === sendPlanExpiredNotification Tests ===

    @Test
    void sendPlanExpiredNotification_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendPlanExpiredNotification("u@t.com", "User", "Vi", "Premium", "9876543210"));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPlanExpiredNotification_MailFailure() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("Auth fail")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class, () ->
                emailService.sendPlanExpiredNotification("u@t.com", "User", "Vi", "Plan", "123"));
    }
}
