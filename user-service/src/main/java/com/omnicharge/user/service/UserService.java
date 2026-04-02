package com.omnicharge.user.service;

import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.DuplicateResourceException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.exception.UnauthorizedException;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.ChangePasswordRequest;
import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogEventPublisher logEventPublisher;

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Track changed fields
        Map<String, String> changedFields = new HashMap<>();
        
        // Check if mobile number is being changed and if it's already taken
        if (request.getMobileNumber() != null && 
            !request.getMobileNumber().equals(user.getMobileNumber())) {
            if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
                throw new DuplicateResourceException("Mobile number already registered");
            }
            changedFields.put("mobileNumber", request.getMobileNumber());
            user.setMobileNumber(request.getMobileNumber());
        }

        if (!request.getFullName().equals(user.getFullName())) {
            changedFields.put("fullName", request.getFullName());
        }
        user.setFullName(request.getFullName());
        userRepository.save(user);

        log.info("Profile updated for user: {}", userId);
        
        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("changedFields", changedFields);
        publishBusinessLog("PROFILE_UPDATE",
            "User profile updated: userId=" + userId + ", fields=" + changedFields,
            context);
        
        return mapToProfileResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify auth provider is LOCAL
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password change is only available for manual registration accounts");
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
        
        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("email", user.getEmail());
        publishBusinessLog("PASSWORD_CHANGE",
            "User password changed: userId=" + userId,
            context);
    }

    // Admin methods
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToProfileResponse);
    }

    public UserProfileResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToProfileResponse(user);
    }

    @Transactional
    public void toggleUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsActive(active);
        userRepository.save(user);

        log.info("User {} status changed to: {}", id, active);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .isActive(user.getIsActive())
                .createdDate(user.getCreatedDate())
                .build();
    }
    
    // Helper method for business operation logging
    private void publishBusinessLog(String eventType, String message, Map<String, Object> context) {
        LogEvent logEvent = LogEvent.builder()
                .serviceName("user-service")
                .level("INFO")
                .logger(this.getClass().getName())
                .message(message)
                .eventType(eventType)
                .context(context)
                .timestamp(LocalDateTime.now())
                .build();
        logEventPublisher.publish(logEvent);
    }
}
