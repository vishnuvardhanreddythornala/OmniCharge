package com.omnicharge.notification.service;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.notification.entity.Notification;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationStatus;
import com.omnicharge.notification.entity.NotificationType;
import com.omnicharge.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for NotificationService business operation logging.
 * 
 * Validates Property 33: Business Operation Event Logging
 * "For any critical business operation (notification sending), the system should log
 * the event with relevant business context including entity IDs, types, and statuses."
 * 
 * This test verifies that all business operations in NotificationService publish
 * log events with appropriate business context.
 */
@ExtendWith(MockitoExtension.class)
@Tag("Feature: production-grade-centralized-logging, Property 33: Business Operation Event Logging")
class NotificationServiceBusinessOperationPropertyTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private IEmailService emailService;

    @Mock
    private ISmsService smsService;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
    }

    @Test
    void property_emailNotificationCreation_shouldLogWithBusinessContext() {
        // Property: For any email notification creation, business context must be logged
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            Long userId = random.nextLong(1, 10000);
            String email = "user" + random.nextInt(10000) + "@example.com";
            String subject = "Test Subject " + random.nextInt(1000);
            String htmlBody = "<html>Test Body</html>";
            NotificationCategory category = getRandomCategory();
            String referenceId = "REF" + random.nextInt(100000);
            
            Notification savedNotification = createNotification(userId, email, NotificationType.EMAIL, category, referenceId);
            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            notificationService.createAndSendEmail(userId, email, subject, htmlBody, category, referenceId);
            
            // Assert: Verify business context is logged
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("NOTIFICATION_CREATED");
            assertThat(capturedEvent.getMessage()).contains("Email notification created successfully");
            
            // Verify business context fields
            Map<String, Object> context = capturedEvent.getContext();
            assertThat(context).isNotNull();
            assertThat(context).containsKey("notificationId");
            assertThat(context).containsKey("userId");
            assertThat(context).containsEntry("type", "EMAIL");
            assertThat(context).containsKey("category");
            assertThat(context).containsEntry("recipient", email);
            assertThat(context).containsEntry("referenceId", referenceId);
            
            // Reset mocks for next iteration
            reset(logEventPublisher, notificationRepository);
        }
    }

    @Test
    void property_smsNotificationSuccess_shouldLogWithDeliveryStatus() {
        // Property: For any successful SMS notification, delivery status must be logged
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            Long userId = random.nextLong(1, 10000);
            String mobile = "+91" + String.format("%010d", random.nextLong(1000000000, 9999999999L));
            String message = "Test SMS " + random.nextInt(1000);
            NotificationCategory category = getRandomCategory();
            String referenceId = "REF" + random.nextInt(100000);
            
            Notification savedNotification = createNotification(userId, mobile, NotificationType.SMS, category, referenceId);
            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
            // SMS sending succeeds (no exception thrown)
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            notificationService.createAndSendSms(userId, mobile, message, category, referenceId);
            
            // Assert: Verify SMS_SENT event with delivery status
            verify(logEventPublisher, atLeast(2)).publish(logEventCaptor.capture());
            
            boolean foundSmsSent = logEventCaptor.getAllValues().stream()
                    .anyMatch(event -> event.getEventType().equals("SMS_SENT"));
            assertThat(foundSmsSent).isTrue();
            
            LogEvent smsSentEvent = logEventCaptor.getAllValues().stream()
                    .filter(event -> event.getEventType().equals("SMS_SENT"))
                    .findFirst()
                    .orElseThrow();
            
            // Verify business context with delivery status
            Map<String, Object> context = smsSentEvent.getContext();
            assertThat(context).containsKey("userId");
            assertThat(context).containsEntry("recipient", mobile);
            assertThat(context).containsKey("category");
            assertThat(context).containsEntry("referenceId", referenceId);
            assertThat(context).containsEntry("deliveryStatus", "SENT");
            
            // Reset mocks for next iteration
            reset(logEventPublisher, notificationRepository, smsService);
        }
    }

    @Test
    void property_smsNotificationFailure_shouldLogWithFailureStatus() {
        // Property: For any failed SMS notification, failure status and reason must be logged
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            Long userId = random.nextLong(1, 10000);
            String mobile = "+91" + String.format("%010d", random.nextLong(1000000000, 9999999999L));
            String message = "Test SMS " + random.nextInt(1000);
            NotificationCategory category = getRandomCategory();
            String referenceId = "REF" + random.nextInt(100000);
            
            Notification savedNotification = createNotification(userId, mobile, NotificationType.SMS, category, referenceId);
            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
            // SMS sending fails
            doThrow(new RuntimeException("SMS sending failed")).when(smsService).sendSms(anyString(), anyString());
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            notificationService.createAndSendSms(userId, mobile, message, category, referenceId);
            
            // Assert: Verify SMS_FAILED event with failure status
            verify(logEventPublisher, atLeast(2)).publish(logEventCaptor.capture());
            
            boolean foundSmsFailed = logEventCaptor.getAllValues().stream()
                    .anyMatch(event -> event.getEventType().equals("SMS_FAILED"));
            assertThat(foundSmsFailed).isTrue();
            
            LogEvent smsFailedEvent = logEventCaptor.getAllValues().stream()
                    .filter(event -> event.getEventType().equals("SMS_FAILED"))
                    .findFirst()
                    .orElseThrow();
            
            // Verify business context with failure status and error message
            Map<String, Object> context = smsFailedEvent.getContext();
            assertThat(context).containsKey("userId");
            assertThat(context).containsEntry("recipient", mobile);
            assertThat(context).containsKey("category");
            assertThat(context).containsEntry("referenceId", referenceId);
            assertThat(context).containsEntry("deliveryStatus", "FAILED");
            assertThat(context).containsKey("errorMessage");
            
            // Reset mocks for next iteration
            reset(logEventPublisher, notificationRepository, smsService);
        }
    }

    @Test
    void property_allBusinessLogs_shouldContainNotificationType() {
        // Property: All notification business logs must contain notification type
        // Run 100+ iterations across different notification types
        
        for (int i = 0; i < 100; i++) {
            Long userId = random.nextLong(1, 10000);
            NotificationCategory category = getRandomCategory();
            String referenceId = "REF" + random.nextInt(100000);
            
            // Randomly choose between email or SMS
            boolean isEmail = random.nextBoolean();
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            if (isEmail) {
                String email = "user" + random.nextInt(10000) + "@example.com";
                Notification savedNotification = createNotification(userId, email, NotificationType.EMAIL, category, referenceId);
                when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
                
                notificationService.createAndSendEmail(userId, email, "Subject", "Body", category, referenceId);
            } else {
                String mobile = "+91" + String.format("%010d", random.nextLong(1000000000, 9999999999L));
                Notification savedNotification = createNotification(userId, mobile, NotificationType.SMS, category, referenceId);
                when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
                
                notificationService.createAndSendSms(userId, mobile, "Message", category, referenceId);
            }
            
            // Assert: All business logs contain type information
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            for (LogEvent event : logEventCaptor.getAllValues()) {
                if (event.getEventType().equals("NOTIFICATION_CREATED") || 
                    event.getEventType().equals("SMS_SENT") || 
                    event.getEventType().equals("SMS_FAILED")) {
                    
                    Map<String, Object> context = event.getContext();
                    assertThat(context).isNotNull();
                    
                    // Verify notification type or category is present
                    boolean hasTypeInfo = context.containsKey("type") || context.containsKey("category");
                    assertThat(hasTypeInfo).isTrue();
                }
            }
            
            // Reset mocks for next iteration
            reset(logEventPublisher, notificationRepository, smsService);
        }
    }

    @Test
    void property_allBusinessLogs_shouldContainRecipientInformation() {
        // Property: All notification business logs must contain recipient information
        // Run 100+ iterations
        
        for (int i = 0; i < 100; i++) {
            Long userId = random.nextLong(1, 10000);
            NotificationCategory category = getRandomCategory();
            String referenceId = "REF" + random.nextInt(100000);
            
            // Randomly choose between email or SMS
            boolean isEmail = random.nextBoolean();
            String recipient;
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            if (isEmail) {
                recipient = "user" + random.nextInt(10000) + "@example.com";
                Notification savedNotification = createNotification(userId, recipient, NotificationType.EMAIL, category, referenceId);
                when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
                
                notificationService.createAndSendEmail(userId, recipient, "Subject", "Body", category, referenceId);
            } else {
                recipient = "+91" + String.format("%010d", random.nextLong(1000000000, 9999999999L));
                Notification savedNotification = createNotification(userId, recipient, NotificationType.SMS, category, referenceId);
                when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
                
                notificationService.createAndSendSms(userId, recipient, "Message", category, referenceId);
            }
            
            // Assert: All business logs contain recipient information
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            for (LogEvent event : logEventCaptor.getAllValues()) {
                if (event.getEventType().equals("NOTIFICATION_CREATED") || 
                    event.getEventType().equals("SMS_SENT") || 
                    event.getEventType().equals("SMS_FAILED")) {
                    
                    Map<String, Object> context = event.getContext();
                    assertThat(context).containsKey("recipient");
                    assertThat(context.get("recipient")).isEqualTo(recipient);
                }
            }
            
            // Reset mocks for next iteration
            reset(logEventPublisher, notificationRepository, smsService);
        }
    }

    // Helper methods to generate random test data
    
    private Notification createNotification(Long userId, String contact, NotificationType type, 
                                           NotificationCategory category, String referenceId) {
        Notification notification = new Notification();
        notification.setId(random.nextLong(1, 100000));
        notification.setUserId(userId);
        
        if (type == NotificationType.EMAIL) {
            notification.setUserEmail(contact);
            notification.setUserMobile("");
        } else {
            notification.setUserMobile(contact);
            notification.setUserEmail("");
        }
        
        notification.setType(type);
        notification.setCategory(category);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setReferenceId(referenceId);
        notification.setStatus(NotificationStatus.SENT);
        notification.setIsRead(false);
        
        return notification;
    }
    
    private NotificationCategory getRandomCategory() {
        NotificationCategory[] categories = NotificationCategory.values();
        return categories[random.nextInt(categories.length)];
    }
}
