package com.omnicharge.notification.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.notification.entity.Notification;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationStatus;
import com.omnicharge.notification.entity.NotificationType;
import com.omnicharge.notification.repository.NotificationRepository;
import com.omnicharge.notification.service.IEmailService;
import com.omnicharge.notification.service.ISmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final JavaMailSender mailSender;
    private final ISmsService smsService;
    private final NotificationRepository notificationRepository;

    @GetMapping("/email")
    public ResponseEntity<ApiResponse<Map<String, String>>> testEmail(
            @RequestParam(defaultValue = "avunashdhanuka@gmail.com") String toEmail) {
        
        Map<String, String> result = new HashMap<>();
        
        try {
            log.info("Testing email to: {}", toEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("OmniCharge - Test Email");
            helper.setText("<html><body><h1>Test Email</h1><p>If you receive this, email configuration is working!</p><p>Timestamp: " + 
                          java.time.LocalDateTime.now() + "</p></body></html>", true);
            
            mailSender.send(message);
            
            result.put("status", "SUCCESS");
            result.put("message", "Email sent successfully to " + toEmail);
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            
            log.info("✅ Test email sent successfully to: {}", toEmail);
            
            return ResponseEntity.ok(ApiResponse.success("Email sent successfully", result));
            
        } catch (Exception e) {
            log.error("❌ Failed to send test email", e);
            
            result.put("status", "FAILED");
            result.put("message", "Failed to send email: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(new ApiResponse<>(false, "Failed to send email", result, java.time.LocalDateTime.now()));
        }
    }

    @GetMapping("/sms")
    public ResponseEntity<ApiResponse<Map<String, String>>> testSms(
            @RequestParam(defaultValue = "+919876543210") String toMobile) {
        
        Map<String, String> result = new HashMap<>();
        
        try {
            log.info("Testing SMS to: {}", toMobile);
            
            String message = "OmniCharge Test SMS - " + java.time.LocalDateTime.now();
            smsService.sendSms(toMobile, message);
            
            result.put("status", "SUCCESS");
            result.put("message", "SMS sent successfully to " + toMobile);
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            
            log.info("✅ Test SMS sent successfully to: {}", toMobile);
            
            return ResponseEntity.ok(ApiResponse.success("SMS sent successfully", result));
            
        } catch (Exception e) {
            log.error("❌ Failed to send test SMS", e);
            
            result.put("status", "FAILED");
            result.put("message", "Failed to send SMS: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(new ApiResponse<>(false, "Failed to send SMS", result, java.time.LocalDateTime.now()));
        }
    }

    @GetMapping("/database")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testDatabase() {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Testing database save...");
            
            // Create test notification
            Notification notification = new Notification();
            notification.setUserId(999L);
            notification.setUserEmail("test@test.com");
            notification.setUserMobile("+919999999999");
            notification.setType(NotificationType.EMAIL);
            notification.setCategory(NotificationCategory.PAYMENT_SUCCESS);
            notification.setSubject("Test Notification");
            notification.setMessage("This is a test notification created at " + java.time.LocalDateTime.now());
            notification.setStatus(NotificationStatus.SENT);
            notification.setReferenceId("TEST-" + System.currentTimeMillis());
            notification.setIsRead(false);
            
            // Save to database
            Notification saved = notificationRepository.save(notification);
            
            result.put("status", "SUCCESS");
            result.put("message", "Notification saved successfully");
            result.put("notificationId", saved.getId());
            result.put("referenceId", saved.getReferenceId());
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            
            // Count total notifications
            long count = notificationRepository.count();
            result.put("totalNotifications", count);
            
            log.info("✅ Test notification saved successfully with ID: {}", saved.getId());
            
            return ResponseEntity.ok(ApiResponse.success("Database test successful", result));
            
        } catch (Exception e) {
            log.error("❌ Failed to save test notification", e);
            
            result.put("status", "FAILED");
            result.put("message", "Failed to save notification: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(new ApiResponse<>(false, "Database test failed", result, java.time.LocalDateTime.now()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testAll() {
        
        Map<String, Object> result = new HashMap<>();
        
        // Test Database
        try {
            Notification notification = new Notification();
            notification.setUserId(999L);
            notification.setUserEmail("test@test.com");
            notification.setUserMobile("+919999999999");
            notification.setType(NotificationType.EMAIL);
            notification.setCategory(NotificationCategory.PAYMENT_SUCCESS);
            notification.setSubject("Test All");
            notification.setMessage("Testing all components");
            notification.setStatus(NotificationStatus.SENT);
            notification.setReferenceId("TEST-ALL-" + System.currentTimeMillis());
            notification.setIsRead(false);
            
            Notification saved = notificationRepository.save(notification);
            result.put("database", "SUCCESS - ID: " + saved.getId());
        } catch (Exception e) {
            result.put("database", "FAILED - " + e.getMessage());
        }
        
        // Test Email
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo("avunashdhanuka@gmail.com");
            helper.setSubject("OmniCharge - Test All");
            helper.setText("<h1>Test All Components</h1>", true);
            mailSender.send(message);
            result.put("email", "SUCCESS");
        } catch (Exception e) {
            result.put("email", "FAILED - " + e.getMessage());
        }
        
        // Test SMS
        try {
            smsService.sendSms("+919876543210", "OmniCharge Test All - " + java.time.LocalDateTime.now());
            result.put("sms", "SUCCESS");
        } catch (Exception e) {
            result.put("sms", "FAILED - " + e.getMessage());
        }
        
        result.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(ApiResponse.success("Test all components completed", result));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getNotificationCount() {
        try {
            long count = notificationRepository.count();
            Map<String, Long> result = new HashMap<>();
            result.put("totalNotifications", count);
            
            return ResponseEntity.ok(ApiResponse.success("Notification count retrieved", result));
        } catch (Exception e) {
            log.error("Failed to get notification count", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to get count"));
        }
    }
}
