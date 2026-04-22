package com.omnicharge.notification.service;

import com.omnicharge.notification.common.exception.BadRequestException;
import com.omnicharge.notification.common.exception.ResourceNotFoundException;
import com.omnicharge.notification.common.logging.LogEventPublisher;
import com.omnicharge.notification.dto.NotificationResponse;
import com.omnicharge.notification.entity.Notification;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationStatus;
import com.omnicharge.notification.entity.NotificationType;
import com.omnicharge.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private IEmailService emailService;
    @Mock private ISmsService smsService;
    @Mock private LogEventPublisher logEventPublisher;

    @InjectMocks private NotificationService notificationService;

    private Notification createNotification(Long id, Long userId) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(userId);
        n.setType(NotificationType.EMAIL);
        n.setCategory(NotificationCategory.PAYMENT_SUCCESS);
        n.setSubject("Test");
        n.setMessage("Test message");
        n.setStatus(NotificationStatus.SENT);
        n.setIsRead(false);
        n.setReferenceId("ref-1");
        n.setUserEmail("test@test.com");
        return n;
    }

    // ===== createAndSendEmail =====

    @Test
    void createAndSendEmail_Success() {
        Notification saved = createNotification(1L, 1L);
        when(notificationRepository.save(any())).thenReturn(saved);

        notificationService.createAndSendEmail(1L, "test@test.com", "Subject", "Body",
                NotificationCategory.PAYMENT_SUCCESS, "ref-1");

        verify(notificationRepository).save(any());
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void createAndSendEmail_DbFailure() {
        when(notificationRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> notificationService.createAndSendEmail(1L, "test@test.com", "Subject", "Body",
                        NotificationCategory.PAYMENT_SUCCESS, "ref-1"));
    }

    // ===== createAndSendSms =====

    @Test
    void createAndSendSms_Success() {
        Notification saved = createNotification(1L, 1L);
        saved.setType(NotificationType.SMS);
        when(notificationRepository.save(any())).thenReturn(saved);

        notificationService.createAndSendSms(1L, "+919876543210", "SMS body",
                NotificationCategory.PAYMENT_SUCCESS, "ref-1");

        verify(smsService).sendSms("+919876543210", "SMS body");
        verify(notificationRepository).save(any());
    }

    @Test
    void createAndSendSms_SmsFailure_SavesFailedStatus() {
        doThrow(new RuntimeException("Twilio error")).when(smsService).sendSms(anyString(), anyString());
        Notification saved = createNotification(1L, 1L);
        saved.setStatus(NotificationStatus.FAILED);
        when(notificationRepository.save(any())).thenReturn(saved);

        notificationService.createAndSendSms(1L, "+919876543210", "SMS body",
                NotificationCategory.PAYMENT_SUCCESS, "ref-1");

        verify(notificationRepository).save(argThat(n -> n.getStatus() == NotificationStatus.FAILED));
    }

    @Test
    void createAndSendSms_DbFailure() {
        when(notificationRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> notificationService.createAndSendSms(1L, "+919876543210", "body",
                        NotificationCategory.PAYMENT_SUCCESS, "ref-1"));
    }

    // ===== getUserNotifications =====

    @Test
    void getUserNotifications_Success() {
        Notification n = createNotification(1L, 1L);
        Page<Notification> page = new PageImpl<>(List.of(n));
        when(notificationRepository.findByUserId(eq(1L), any())).thenReturn(page);

        Page<NotificationResponse> result = notificationService.getUserNotifications(1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    // ===== markAsRead =====

    @Test
    void markAsRead_Success() {
        Notification n = createNotification(1L, 1L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenReturn(n);

        notificationService.markAsRead(1L, 1L);

        verify(notificationRepository).save(argThat(saved -> saved.getIsRead()));
    }

    @Test
    void markAsRead_NotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(999L, 1L));
    }

    @Test
    void markAsRead_UnauthorizedUser() {
        Notification n = createNotification(1L, 1L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThrows(BadRequestException.class, () -> notificationService.markAsRead(1L, 999L));
    }

    // ===== getUnreadCount =====

    @Test
    void getUnreadCount_Success() {
        when(notificationRepository.countByUserIdAndIsRead(1L, false)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(5, count);
    }

    // ===== getAllNotifications =====

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"ALL", "INVALID_GARBAGE"})
    void getAllNotifications_FallsBackToAll(String category) {
        Page<Notification> page = new PageImpl<>(List.of());
        when(notificationRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<NotificationResponse> result = notificationService.getAllNotifications(category, PageRequest.of(0, 10));
        assertNotNull(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "SYSTEM", "PAYMENT_SUCCESS"})
    void getAllNotifications_WithCategory(String category) {
        Page<Notification> page = new PageImpl<>(List.of());
        when(notificationRepository.findByCategoryIn(anyList(), any())).thenReturn(page);

        Page<NotificationResponse> result = notificationService.getAllNotifications(category, PageRequest.of(0, 10));
        assertNotNull(result);
    }
}
