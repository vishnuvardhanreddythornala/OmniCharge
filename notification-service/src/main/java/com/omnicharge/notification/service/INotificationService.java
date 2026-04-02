package com.omnicharge.notification.service;

import com.omnicharge.notification.dto.NotificationResponse;
import com.omnicharge.notification.entity.NotificationCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface INotificationService {

    void createAndSendEmail(Long userId, String email, String subject, String htmlBody, 
                           NotificationCategory category, String referenceId);

    void createAndSendSms(Long userId, String mobile, String message, 
                         NotificationCategory category, String referenceId);

    Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);

    void markAsRead(Long id, Long userId);

    long getUnreadCount(Long userId);

    Page<NotificationResponse> getAllNotifications(Pageable pageable);
}
