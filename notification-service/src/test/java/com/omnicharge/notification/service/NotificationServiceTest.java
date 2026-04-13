package com.omnicharge.notification.service;

import com.omnicharge.notification.common.exception.BadRequestException;
import com.omnicharge.notification.common.exception.ResourceNotFoundException;
import com.omnicharge.notification.common.logging.LogEvent;
import com.omnicharge.notification.common.logging.LogEventPublisher;
import com.omnicharge.notification.dto.NotificationResponse;
import com.omnicharge.notification.entity.Notification;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationStatus;
import com.omnicharge.notification.entity.NotificationType;
import com.omnicharge.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private IEmailService emailService;
    @Mock private ISmsService smsService;
    @Mock private LogEventPublisher logEventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setId(1L);
        notification.setUserId(10L);
        notification.setType(NotificationType.EMAIL);
        notification.setCategory(NotificationCategory.PAYMENT_SUCCESS);
        notification.setStatus(NotificationStatus.SENT);
        notification.setIsRead(false);
    }

    @Test
    void createAndSendEmail_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.createAndSendEmail(10L, "test@test.com", "Sub", "Body", NotificationCategory.PAYMENT_SUCCESS, "ref-1");

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void createAndSendEmail_FailsToSave() {
        when(notificationRepository.save(any(Notification.class))).thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class, () -> 
            notificationService.createAndSendEmail(10L, "test@test.com", "Sub", "Body", NotificationCategory.PAYMENT_SUCCESS, "ref-1")
        );
    }

    @Test
    void createAndSendSms_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.createAndSendSms(10L, "9876543210", "Message", NotificationCategory.PAYMENT_SUCCESS, "ref-1");

        verify(smsService, times(1)).sendSms("9876543210", "Message");
        verify(notificationRepository, times(1)).save(argThat(n -> n.getStatus() == NotificationStatus.SENT));
    }

    @Test
    void createAndSendSms_SmsFailureThrowsErrorButSavesFailedStatus() {
        doThrow(new RuntimeException("Twilio down")).when(smsService).sendSms("9876543210", "Message");
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.createAndSendSms(10L, "9876543210", "Message", NotificationCategory.PAYMENT_SUCCESS, "ref-1");

        verify(smsService, times(1)).sendSms("9876543210", "Message");
        verify(notificationRepository, times(1)).save(argThat(n -> n.getStatus() == NotificationStatus.FAILED));
        // Expecting 2 log events: SMS_FAILED and NOTIFICATION_CREATED
        verify(logEventPublisher, times(2)).publish(any(LogEvent.class));
    }

    @Test
    void getUserNotifications_Success() {
        Page<Notification> p = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserId(10L, PageRequest.of(0, 10))).thenReturn(p);

        Page<NotificationResponse> result = notificationService.getUserNotifications(10L, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
    }

    @Test
    void markAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, 10L);

        assertTrue(notification.getIsRead());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void markAsRead_Unauthorized() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(BadRequestException.class, () -> notificationService.markAsRead(1L, 99L));
    }

    @Test
    void markAsRead_NotFound() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(1L, 10L));
    }

    @Test
    void getUnreadCount_Success() {
        when(notificationRepository.countByUserIdAndIsRead(10L, false)).thenReturn(5L);

        assertEquals(5L, notificationService.getUnreadCount(10L));
    }

    @Test
    void getAllNotifications_UserCategory() {
        Page<Notification> p = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByCategoryIn(anyList(), any())).thenReturn(p);

        Page<NotificationResponse> result = notificationService.getAllNotifications("USER", PageRequest.of(0, 10));
        assertEquals(1, result.getContent().size());
    }
}
