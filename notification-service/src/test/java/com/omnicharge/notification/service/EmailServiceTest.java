package com.omnicharge.notification.service;

import com.omnicharge.notification.common.event.PaymentCompletedEvent;
import com.omnicharge.notification.common.event.RechargeCompletedEvent;
import com.omnicharge.notification.common.logging.LogEventPublisher;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private LogEventPublisher logEventPublisher;
    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp()  {
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@omnicharge.com");
        ReflectionTestUtils.setField(emailService, "mailPassword", "testLogin@630");
    }

    // ===== sendPaymentConfirmation =====

    @Test
    void sendPaymentConfirmation_Success() throws Exception {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("txn-1").rechargeId("rec-1").status("SUCCESS")
                .amount(new BigDecimal("100")).mobileNumber("9876543210")
                .operatorName("Jio").planName("Gold").paymentMethod("UPI")
                .timestamp(LocalDateTime.now()).build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPaymentConfirmation("test@test.com", event);

        verify(mailSender, times(1)).send(mimeMessage);
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_SENT")));
    }

    @Test
    void sendPaymentConfirmation_FailedStatus() throws Exception {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("txn-2").rechargeId("rec-2").status("FAILED")
                .amount(new BigDecimal("200")).mobileNumber("9876543210")
                .operatorName("Airtel").planName("Silver").paymentMethod("CARD")
                .timestamp(LocalDateTime.now()).build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPaymentConfirmation("test@test.com", event);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendPaymentConfirmation_SmtpFailure() throws Exception {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("txn-1").status("SUCCESS").amount(new BigDecimal("100"))
                .timestamp(LocalDateTime.now()).build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP ERROR")).when(mailSender).send(mimeMessage);

        assertThrows(RuntimeException.class, () -> emailService.sendPaymentConfirmation("test@test.com", event));
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_FAILED")));
    }

    // ===== sendRechargeConfirmation =====

    @Test
    void sendRechargeConfirmation_Success() throws Exception {
        RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                .rechargeId("rec-1").status("SUCCESS").amount(new BigDecimal("199"))
                .mobileNumber("9876543210").operatorName("Vi").planName("Unlimited")
                .transactionId("txn-x").timestamp(LocalDateTime.now()).build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRechargeConfirmation("test@test.com", event);

        verify(mailSender, times(1)).send(mimeMessage);
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_SENT")));
    }

    @Test
    void sendRechargeConfirmation_FailedStatus() throws Exception {
        RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                .rechargeId("rec-2").status("FAILED").amount(new BigDecimal("199"))
                .mobileNumber("9876543210").operatorName("BSNL").planName("Basic")
                .transactionId("txn-fail").timestamp(LocalDateTime.now()).build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRechargeConfirmation("test@test.com", event);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendRechargeConfirmation_SmtpFailure() throws Exception {
        RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                .rechargeId("rec-1").status("SUCCESS").timestamp(LocalDateTime.now()).build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP ERROR")).when(mailSender).send(mimeMessage);

        assertThrows(RuntimeException.class, () -> emailService.sendRechargeConfirmation("test@test.com", event));
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_FAILED")));
    }

    // ===== sendPlanExpiryReminder =====

    @Test
    void sendPlanExpiryReminder_Success() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPlanExpiryReminder("test@test.com", "John", "Airtel", "Unlimited", "9876543210", 5);

        verify(mailSender, times(1)).send(mimeMessage);
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_SENT")));
    }

    @Test
    void sendPlanExpiryReminder_SmtpFailure() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP ERROR")).when(mailSender).send(mimeMessage);

        assertThrows(RuntimeException.class,
                () -> emailService.sendPlanExpiryReminder("test@test.com", "John", "Airtel", "Unlimited", "9876543210",
                        3));
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_FAILED")));
    }

    // ===== sendPlanExpiredNotification =====

    @Test
    void sendPlanExpiredNotification_Success() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPlanExpiredNotification("test@test.com", "John", "Airtel", "Unlimited", "9876543210");

        verify(mailSender, times(1)).send(mimeMessage);
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_SENT")));
    }

    @Test
    void sendPlanExpiredNotification_SmtpFailure() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP ERROR")).when(mailSender).send(mimeMessage);

        assertThrows(RuntimeException.class,
                () -> emailService.sendPlanExpiredNotification("test@test.com", "John", "Airtel", "Unlimited",
                        "9876543210"));
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_FAILED")));
    }

    // ===== init =====

    @Test
    void init_LogsConfiguration()  {
        assertDoesNotThrow(() -> emailService.init());
    }

    @Test
    void init_NullPassword()  {
        ReflectionTestUtils.setField(emailService, "mailPassword", null);
        assertDoesNotThrow(() -> emailService.init());
    }

    @Test
    void init_ShortPassword()  {
        ReflectionTestUtils.setField(emailService, "mailPassword", "ab");
        assertDoesNotThrow(() -> emailService.init());
    }
}
