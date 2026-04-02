package com.omnicharge.user.service;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.ChangePasswordRequest;
import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for UserService log persistence.
 * 
 * Validates Property 1: Universal Service Log Persistence
 * "For any microservice in the OmniCharge system, when it generates a log event,
 * that event should be received by the Logging_Service and persisted to both
 * the database and the appropriate log file."
 * 
 * This test verifies that all business operations in UserService publish log events
 * to RabbitMQ via LogEventPublisher.
 */
@ExtendWith(MockitoExtension.class)
@Tag("Feature: production-grade-centralized-logging, Property 1: Universal Service Log Persistence")
class UserServiceLogPersistencePropertyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private UserService userService;

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
    }

    @Test
    void property_updateProfile_shouldAlwaysPublishLogEvent() {
        // Property: For any profile update operation, a log event must be published
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange: Generate random user and update request
            User user = createRandomUser();
            UpdateProfileRequest request = createRandomUpdateRequest();
            
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.existsByMobileNumber(anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            userService.updateProfile(user.getId(), request);
            
            // Assert: Verify log event was published
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent).isNotNull();
            assertThat(capturedEvent.getServiceName()).isEqualTo("user-service");
            assertThat(capturedEvent.getLevel()).isEqualTo("INFO");
            assertThat(capturedEvent.getEventType()).isEqualTo("PROFILE_UPDATE");
            assertThat(capturedEvent.getMessage()).contains("User profile updated");
            assertThat(capturedEvent.getMessage()).contains("userId=" + user.getId());
            assertThat(capturedEvent.getTimestamp()).isNotNull();
            
            // Reset mocks for next iteration
            reset(logEventPublisher, userRepository);
        }
    }

    @Test
    void property_changePassword_shouldAlwaysPublishLogEvent() {
        // Property: For any password change operation, a log event must be published
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange: Generate random user and password change request
            User user = createRandomUser();
            user.setAuthProvider(AuthProvider.LOCAL); // Only LOCAL users can change password
            ChangePasswordRequest request = createRandomPasswordChangeRequest();
            
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            userService.changePassword(user.getId(), request);
            
            // Assert: Verify log event was published
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent).isNotNull();
            assertThat(capturedEvent.getServiceName()).isEqualTo("user-service");
            assertThat(capturedEvent.getLevel()).isEqualTo("INFO");
            assertThat(capturedEvent.getEventType()).isEqualTo("PASSWORD_CHANGE");
            assertThat(capturedEvent.getMessage()).contains("User password changed");
            assertThat(capturedEvent.getMessage()).contains("userId=" + user.getId());
            assertThat(capturedEvent.getTimestamp()).isNotNull();
            
            // Reset mocks for next iteration
            reset(logEventPublisher, userRepository, passwordEncoder);
        }
    }

    @Test
    void property_allLogEvents_shouldHaveRequiredFields() {
        // Property: All log events must have serviceName, level, message, and timestamp
        // Run 100+ iterations across different operations
        
        for (int i = 0; i < 100; i++) {
            User user = createRandomUser();
            user.setAuthProvider(AuthProvider.LOCAL);
            
            // Randomly choose between update profile or change password
            boolean isUpdateProfile = random.nextBoolean();
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            if (isUpdateProfile) {
                UpdateProfileRequest request = createRandomUpdateRequest();
                when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
                when(userRepository.existsByMobileNumber(anyString())).thenReturn(false);
                when(userRepository.save(any(User.class))).thenReturn(user);
                
                userService.updateProfile(user.getId(), request);
            } else {
                ChangePasswordRequest request = createRandomPasswordChangeRequest();
                when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
                when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                
                userService.changePassword(user.getId(), request);
            }
            
            // Assert: Verify all required fields are present
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent.getServiceName()).isNotNull().isNotEmpty();
            assertThat(capturedEvent.getLevel()).isNotNull().isNotEmpty();
            assertThat(capturedEvent.getMessage()).isNotNull().isNotEmpty();
            assertThat(capturedEvent.getTimestamp()).isNotNull();
            assertThat(capturedEvent.getEventType()).isNotNull().isNotEmpty();
            assertThat(capturedEvent.getLogger()).isNotNull().isNotEmpty();
            
            // Reset mocks for next iteration
            reset(logEventPublisher, userRepository, passwordEncoder);
        }
    }

    // Helper methods to generate random test data
    
    private User createRandomUser() {
        User user = new User();
        user.setId(random.nextLong(1, 10000));
        user.setEmail("user" + random.nextInt(10000) + "@example.com");
        user.setFullName("User " + random.nextInt(10000));
        user.setMobileNumber("98765" + String.format("%05d", random.nextInt(100000)));
        user.setRole(Role.ROLE_USER);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setIsActive(true);
        user.setPassword("encodedPassword");
        return user;
    }
    
    private UpdateProfileRequest createRandomUpdateRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name " + random.nextInt(10000));
        request.setMobileNumber("99999" + String.format("%05d", random.nextInt(100000)));
        return request;
    }
    
    private ChangePasswordRequest createRandomPasswordChangeRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword" + random.nextInt(1000));
        request.setNewPassword("newPassword" + random.nextInt(1000));
        return request;
    }
}
