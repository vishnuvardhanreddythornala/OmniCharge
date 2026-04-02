package com.omnicharge.notification.service;

import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.notification.dto.NotificationResponse;
import com.omnicharge.notification.entity.*;
import com.omnicharge.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private IEmailService emailService;

    @Mock
    private ISmsService smsService;

    @InjectMocks
    private NotificationService notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = new Notification();
        sampleNotification.setId(1L);
        sampleNotification.setUserId(10L);
        sampleNotification.setUserEmail("user@test.com");
        sampleNotification.setUserMobile("9876543210");
        sampleNotification.setType(NotificationType.EMAIL);
        sampleNotification.setCategory(NotificationCategory.PAYMENT_SUCCESS);
        sampleNotification.setSubject("Payment Confirmed");
        sampleNotification.setMessage("Your payment was successful");
        sampleNotification.setStatus(NotificationStatus.SENT);
        sampleNotification.setReferenceId("TXN-1");
        sampleNotification.setIsRead(false);
        sampleNotification.setCreatedDate(LocalDateTime.now());
    }

    // === createAndSendEmail Tests ===

    @Test
    void createAndSendEmail_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        notificationService.createAndSendEmail(10L, "user@test.com", "Subject", "Body",
                NotificationCategory.PAYMENT_SUCCESS, "TXN-1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationType.EMAIL, captor.getValue().getType());
        assertEquals(NotificationStatus.SENT, captor.getValue().getStatus());
        assertEquals("TXN-1", captor.getValue().getReferenceId());
    }

    @Test
    void createAndSendEmail_DbFailure_Rethrows() {
        when(notificationRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThrows(RuntimeException.class, () ->
                notificationService.createAndSendEmail(10L, "u@t.com", "S", "B",
                        NotificationCategory.PAYMENT_FAILED, "REF-1"));
    }

    @Test
    void createAndSendEmail_SetsIsReadFalse() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.createAndSendEmail(10L, "u@t.com", "S", "B",
                NotificationCategory.PLAN_EXPIRY_REMINDER, "R-1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertFalse(captor.getValue().getIsRead());
    }

    // === createAndSendSms Tests ===

    @Test
    void createAndSendSms_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        notificationService.createAndSendSms(10L, "9876543210", "Your recharge is done",
                NotificationCategory.PAYMENT_SUCCESS, "TXN-2");

        verify(smsService).sendSms("9876543210", "Your recharge is done");
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationStatus.SENT, captor.getValue().getStatus());
        assertEquals(NotificationType.SMS, captor.getValue().getType());
    }

    @Test
    void createAndSendSms_SmsFailure_StatusFailed() {
        doThrow(new RuntimeException("Twilio error")).when(smsService).sendSms(anyString(), anyString());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.createAndSendSms(10L, "9876543210", "msg",
                NotificationCategory.PAYMENT_FAILED, "TXN-3");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    void createAndSendSms_DbFailure_Rethrows() {
        when(notificationRepository.save(any())).thenThrow(new RuntimeException("DB Crash"));

        assertThrows(RuntimeException.class, () ->
                notificationService.createAndSendSms(10L, "9876543210", "msg",
                        NotificationCategory.PAYMENT_SUCCESS, "R-1"));
    }

    @Test
    void createAndSendSms_SetsEmptyEmailForSms() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.createAndSendSms(10L, "9876543210", "msg",
                NotificationCategory.PAYMENT_SUCCESS, "R-1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("", captor.getValue().getUserEmail());
    }

    // === getUserNotifications Tests ===

    @Test
    void getUserNotifications_ReturnsMappedPage() {
        Page<Notification> page = new PageImpl<>(Collections.singletonList(sampleNotification));
        when(notificationRepository.findByUserId(eq(10L), any(Pageable.class))).thenReturn(page);

        Page<NotificationResponse> result = notificationService.getUserNotifications(10L, PageRequest.of(0, 10));

        assertFalse(result.isEmpty());
        assertEquals("TXN-1", result.getContent().get(0).getReferenceId());
        assertEquals(NotificationType.EMAIL, result.getContent().get(0).getType());
    }

    @Test
    void getUserNotifications_EmptyPage() {
        Page<Notification> emptyPage = new PageImpl<>(Collections.emptyList());
        when(notificationRepository.findByUserId(eq(99L), any(Pageable.class))).thenReturn(emptyPage);

        Page<NotificationResponse> result = notificationService.getUserNotifications(99L, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    // === markAsRead Tests ===

    @Test
    void markAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any())).thenReturn(sampleNotification);

        notificationService.markAsRead(1L, 10L);

        assertTrue(sampleNotification.getIsRead());
        verify(notificationRepository).save(sampleNotification);
    }

    @Test
    void markAsRead_NotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                notificationService.markAsRead(999L, 10L));
    }

    @Test
    void markAsRead_UnauthorizedUser() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

        assertThrows(BadRequestException.class, () ->
                notificationService.markAsRead(1L, 99L));  // userId mismatch
    }

    // === getUnreadCount Tests ===

    @Test
    void getUnreadCount_ReturnsCorrect() {
        when(notificationRepository.countByUserIdAndIsRead(10L, false)).thenReturn(5L);

        long count = notificationService.getUnreadCount(10L);

        assertEquals(5L, count);
    }

    @Test
    void getUnreadCount_ZeroForNoNotifications() {
        when(notificationRepository.countByUserIdAndIsRead(10L, false)).thenReturn(0L);

        assertEquals(0L, notificationService.getUnreadCount(10L));
    }

    // === getAllNotifications Tests ===

    @Test
    void getAllNotifications_ReturnsMappedPage() {
        Page<Notification> page = new PageImpl<>(Collections.singletonList(sampleNotification));
        when(notificationRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<NotificationResponse> result = notificationService.getAllNotifications(PageRequest.of(0, 10));

        assertFalse(result.isEmpty());
        assertEquals(1, result.getContent().size());
    }
}
