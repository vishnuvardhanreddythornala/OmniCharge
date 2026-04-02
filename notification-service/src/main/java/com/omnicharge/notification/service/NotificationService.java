package com.omnicharge.notification.service;

import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.notification.dto.NotificationResponse;
import com.omnicharge.notification.entity.Notification;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationStatus;
import com.omnicharge.notification.entity.NotificationType;
import com.omnicharge.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final IEmailService emailService;
    private final ISmsService smsService;
    private final LogEventPublisher logEventPublisher;

    @Override
    @Transactional
    public void createAndSendEmail(Long userId, String email, String subject, String htmlBody, 
                                   NotificationCategory category, String referenceId) {
        log.info("Creating email notification - userId: {}, email: {}, category: {}, refId: {}", 
                userId, email, category, referenceId);
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserEmail(email);
        notification.setType(NotificationType.EMAIL);
        notification.setCategory(category);
        notification.setSubject(subject);
        notification.setMessage(htmlBody);
        notification.setReferenceId(referenceId);
        notification.setStatus(NotificationStatus.SENT);  // Email already sent by consumer
        notification.setIsRead(false);

        try {
            Notification saved = notificationRepository.save(notification);
            log.info("✅ Email notification saved successfully with ID: {}", saved.getId());
            
            // Log business operation
            publishBusinessLog("NOTIFICATION_CREATED", "Email notification created successfully", Map.of(
                    "notificationId", saved.getId().toString(),
                    "userId", userId.toString(),
                    "type", "EMAIL",
                    "category", category.toString(),
                    "recipient", email,
                    "referenceId", referenceId
            ));
        } catch (Exception e) {
            log.error("❌ Failed to save email notification to database", e);
            throw e;  // Re-throw to see the error in logs
        }
    }

    @Override
    @Transactional
    public void createAndSendSms(Long userId, String mobile, String message, 
                                 NotificationCategory category, String referenceId) {
        log.info("Creating SMS notification - userId: {}, mobile: {}, category: {}, refId: {}", 
                userId, mobile, category, referenceId);
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserMobile(mobile);
        notification.setUserEmail(""); // SMS doesn't need email
        notification.setType(NotificationType.SMS);
        notification.setCategory(category);
        notification.setSubject("SMS Notification");
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setIsRead(false);

        try {
            smsService.sendSms(mobile, message);
            notification.setStatus(NotificationStatus.SENT);
            log.info("✅ SMS sent successfully to: {}", mobile);
            
            // Log business operation - SMS sent successfully
            publishBusinessLog("SMS_SENT", "SMS sent successfully", Map.of(
                    "userId", userId.toString(),
                    "recipient", mobile,
                    "category", category.toString(),
                    "referenceId", referenceId,
                    "deliveryStatus", "SENT"
            ));
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            log.error("❌ Failed to send SMS to: {}", mobile, e);
            
            // Log business operation - SMS failed
            publishBusinessLog("SMS_FAILED", "SMS sending failed", Map.of(
                    "userId", userId.toString(),
                    "recipient", mobile,
                    "category", category.toString(),
                    "referenceId", referenceId,
                    "deliveryStatus", "FAILED",
                    "errorMessage", e.getMessage()
            ));
        }

        try {
            Notification saved = notificationRepository.save(notification);
            log.info("✅ SMS notification saved successfully with ID: {}", saved.getId());
            
            // Log notification creation
            publishBusinessLog("NOTIFICATION_CREATED", "SMS notification created successfully", Map.of(
                    "notificationId", saved.getId().toString(),
                    "userId", userId.toString(),
                    "type", "SMS",
                    "category", category.toString(),
                    "recipient", mobile,
                    "referenceId", referenceId,
                    "status", saved.getStatus().toString()
            ));
        } catch (Exception e) {
            log.error("❌ Failed to save SMS notification to database", e);
            throw e;  // Re-throw to see the error in logs
        }
    }

    @Override
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserId(userId, pageable);
        return notifications.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (!notification.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to notification");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        log.info("Notification marked as read: {}", id);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Override
    public Page<NotificationResponse> getAllNotifications(Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findAll(pageable);
        return notifications.map(this::mapToResponse);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .category(notification.getCategory())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdDate(notification.getCreatedDate())
                .build();
    }

    /**
     * Helper method to publish business operation logs to centralized logging system
     */
    private void publishBusinessLog(String eventType, String message, Map<String, String> context) {
        try {
            LogEvent logEvent = new LogEvent();
            logEvent.setServiceName("notification-service");
            logEvent.setLevel("INFO");
            logEvent.setMessage(message);
            logEvent.setEventType(eventType);
            logEvent.setContext(new HashMap<>(context)); // Convert Map<String,String> to Map<String,Object>
            logEvent.setLogger(this.getClass().getName());
            logEvent.setTimestamp(java.time.LocalDateTime.now());
            
            logEventPublisher.publish(logEvent);
        } catch (Exception e) {
            log.error("Failed to publish business log for event: {}", eventType, e);
            // Don't throw - logging failure shouldn't break business operations
        }
    }
}
