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

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for NotificationService log persistence.
 * 
 * Validates Property 1: Universal Service Log Persistence
 * "For any microservice in the OmniCharge system, when it generates a log event,
 * that event should be received by the Logging_Service and persisted to both
 * the database and the appropriate log file."
 * 
 * This test verifies that all business operations in NotificationService publish log events
 * to RabbitMQ via LogEventPublisher.
 */
@ExtendWith(MockitoExtension.class)
@Tag("Feature: production-grade-centralized-logging, Property 1: Universal Service Log Persistence")
class NotificationServiceLogPersistencePropertyTest {

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
    void property_createAndSendEmail_shouldAlwaysPublishLogEvent() {
        // Property: For any email notification creation, a log event must be published
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange: Generate random email notification data
            Long userId = random.nextLong(1, 10000);
            String email = "user" + random.nextInt(10000) + "@example.com";
            String subject = "Test Subject " + random.nextInt(1000);
            String htmlBody = "<html>Test Body " + random.nextInt(1000) + "</html>";
            NotificationCategory category = getRandomCategory();
            String referenceId = "REF" + random.nextInt(100000);
            
            Notification savedNotification = createNotification(userId, email, NotificationType.EMAIL, category, referenceId);
            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            notificationService.createAndSendEmail(userId, email, subject, htmlBody, category, referenceId);
            
            // Assert: Verify log event was published
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent).isNotNull();
            assertThat(capturedEvent.getServiceName()).isEqualTo("notification-service");
            assertThat(capturedEvent.getLevel()).isEqualTo("INFO");
            assertThat(capturedEvent.getEventType()).isEqualTo("NOTIFICATION_CREATED");
            assertThat(capturedEvent.getMessage()).contains("Email notification created successfully");
            assertThat(capturedEvent.getTimestamp()).isNotNull();
            
            // Reset mocks for next iteration
            reset(logEventPublisher, notificationRepository);
        }
    }

    @Test
    void property_createAndSendSms_shouldAlwaysPublishLogEvent() {
        // Property: For any SMS notification creation, log events must be published
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange: Generate random SMS notification data
            Long userId = random.nextLong(1, 10000);
            String mobile = "+91" + String.format("%010d", random.nextLong(1000000000, 9999999999L));
            String message = "Test SMS message " + random.nextInt(1000);
            NotificationCategory category = getRandomCategory();
            String referenceId = "REF" + random.nextInt(100000);
            
            Notification savedNotification = createNotification(userId, mobile, NotificationType.SMS, category, referenceId);
            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
            
            // Randomly simulate SMS success or failure
            boolean smsSuccess = random.nextBoolean();
            if (!smsSuccess) {
                doThrow(new RuntimeException("SMS sending failed")).when(smsService).sendSms(anyString(), anyString());
            }
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            notificationService.createAndSendSms(userId, mobile, message, category, referenceId);
            
            // Assert: Verify log events were published (SMS_SENT/SMS_FAILED + NOTIFICATION_CREATED)
            verify(logEventPublisher, atLeast(2)).publish(logEventCaptor.capture());
            
            // Check that at least one log event has the expected structure
            boolean foundSmsEvent = logEventCaptor.getAllValues().stream()
                    .anyMatch(event -> event.getEventType().equals("SMS_SENT") || event.getEventType().equals("SMS_FAILED"));
            boolean foundNotificationCreated = logEventCaptor.getAllValues().stream()
                    .anyMatch(event -> event.getEventType().equals("NOTIFICATION_CREATED"));
            
            assertThat(foundSmsEvent).isTrue();
            assertThat(foundNotificationCreated).isTrue();
            
            // Verify all events have required fields
            for (LogEvent event : logEventCaptor.getAllValues()) {
                assertThat(event.getServiceName()).isEqualTo("notification-service");
                assertThat(event.getLevel()).isIn("INFO", "WARN");
                assertThat(event.getTimestamp()).isNotNull();
            }
            
            // Reset mocks for next iteration
            reset(logEventPublisher, notificationRepository, smsService);
        }
    }

    @Test
    void property_allLogEvents_shouldHaveRequiredFields() {
        // Property: All log events must have serviceName, level, message, timestamp, eventType, and logger
        // Run 100+ iterations across different operations
        
        for (int i = 0; i < 100; i++) {
            Long userId = random.nextLong(1, 10000);
            NotificationCategory category = getRandomCategory();
            String referenceId = "REF" + random.nextInt(100000);
            
            // Randomly choose between email or SMS
            boolean isEmail = random.nextBoolean();
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            if (isEmail) {
                String email = "user" + random.nextInt(10000) + "@example.com";
                String subject = "Test Subject " + random.nextInt(1000);
                String htmlBody = "<html>Test Body " + random.nextInt(1000) + "</html>";
                
                Notification savedNotification = createNotification(userId, email, NotificationType.EMAIL, category, referenceId);
                when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
                
                notificationService.createAndSendEmail(userId, email, subject, htmlBody, category, referenceId);
            } else {
                String mobile = "+91" + String.format("%010d", random.nextLong(1000000000, 9999999999L));
                String message = "Test SMS message " + random.nextInt(1000);
                
                Notification savedNotification = createNotification(userId, mobile, NotificationType.SMS, category, referenceId);
                when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
                
                notificationService.createAndSendSms(userId, mobile, message, category, referenceId);
            }
            
            // Assert: Verify all required fields are present in all log events
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            for (LogEvent capturedEvent : logEventCaptor.getAllValues()) {
                assertThat(capturedEvent.getServiceName()).isNotNull().isNotEmpty();
                assertThat(capturedEvent.getLevel()).isNotNull().isNotEmpty();
                assertThat(capturedEvent.getMessage()).isNotNull().isNotEmpty();
                assertThat(capturedEvent.getTimestamp()).isNotNull();
                assertThat(capturedEvent.getEventType()).isNotNull().isNotEmpty();
                assertThat(capturedEvent.getLogger()).isNotNull().isNotEmpty();
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
