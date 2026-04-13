package com.omnicharge.notification.service;

import com.omnicharge.notification.common.event.PaymentCompletedEvent;
import com.omnicharge.notification.common.event.RechargeCompletedEvent;
import com.omnicharge.notification.common.logging.LogEvent;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private LogEventPublisher logEventPublisher;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@omnicharge.com");
    }

    @Test
    void sendPaymentConfirmation_Success() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("txn-1")
                .status("SUCCESS")
                .amount(new BigDecimal("100"))
                .timestamp(LocalDateTime.now())
                .build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPaymentConfirmation("test@test.com", event);

        verify(mailSender, times(1)).send(mimeMessage);
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_SENT")));
    }

    @Test
    void sendPaymentConfirmation_Failure() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("txn-1")
                .status("SUCCESS")
                .amount(new BigDecimal("100"))
                .timestamp(LocalDateTime.now())
                .build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP ERROR")).when(mailSender).send(mimeMessage);

        assertThrows(RuntimeException.class, () -> emailService.sendPaymentConfirmation("test@test.com", event));
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_FAILED")));
    }

    @Test
    void sendRechargeConfirmation_Success() {
        RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                .rechargeId("rec-1")
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRechargeConfirmation("test@test.com", event);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendPlanExpiryReminder_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPlanExpiryReminder("test@test.com", "John", "Airtel", "Unlimited", "9876543210", 3);

        verify(mailSender, times(1)).send(mimeMessage);
        verify(logEventPublisher, times(1)).publish(argThat(log -> log.getEventType().equals("EMAIL_SENT")));
    }

    @Test
    void sendPlanExpiredNotification_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPlanExpiredNotification("test@test.com", "John", "Airtel", "Unlimited", "9876543210");

        verify(mailSender, times(1)).send(mimeMessage);
    }
}
