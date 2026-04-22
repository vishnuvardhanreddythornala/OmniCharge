package com.omnicharge.user.service;

import com.omnicharge.user.common.exception.BadRequestException;
import com.omnicharge.user.common.exception.ResourceNotFoundException;
import com.omnicharge.user.common.exception.UnauthorizedException;
import com.omnicharge.user.common.logging.LogEvent;
import com.omnicharge.user.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.RefreshToken;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.RefreshTokenRepository;
import com.omnicharge.user.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements InterfaceUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogEventPublisher logEventPublisher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, String> redisTemplate;

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
        
        // SECURITY LOCKDOWN: Mobile number can ONLY be updated via /verify-mobile endpoint
        // This prevents users from changing mobile numbers without verification
        
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


    // Admin methods
    public Page<UserProfileResponse> getAllUsers(String search, String status, Pageable pageable) {
        // Determine if we need status filtering
        final Boolean isActiveFilter;
        if ("ACTIVE".equalsIgnoreCase(status)) {
            isActiveFilter = true;
        } else if ("SUSPENDED".equalsIgnoreCase(status)) {
            isActiveFilter = false;
        } else {
            isActiveFilter = null;
        }

        if (search != null && !search.trim().isEmpty()) {
            String cleanSearch = search.trim();
            
            // Handle USR- prefix or zero-padded numeric IDs (e.g., USR-00025, 00025)
            String idCandidate = cleanSearch;
            if (idCandidate.toUpperCase().startsWith("USR-")) {
                idCandidate = idCandidate.substring(4);
            }
            // Check if the remaining string is purely numeric (with optional leading zeros)
            if (idCandidate.matches("^\\d+$")) {
                try {
                    Long id = Long.parseLong(idCandidate);
                    return userRepository.findById(id)
                        .filter(u -> isActiveFilter == null || u.getIsActive().equals(isActiveFilter))
                        .map(u -> new org.springframework.data.domain.PageImpl<>(
                                java.util.Collections.singletonList(u), pageable, 1))
                        .orElseGet(() -> new org.springframework.data.domain.PageImpl<>(
                                java.util.Collections.emptyList(), pageable, 0))
                        .map(this::mapToProfileResponse);
                } catch (NumberFormatException e) {
                    // Fall through to regular search
                }
            }
            
            // Regular fuzzy search for names and emails
            if (isActiveFilter != null) {
                return userRepository.searchUsersByStatus(cleanSearch, isActiveFilter, pageable)
                        .map(this::mapToProfileResponse);
            }
            return userRepository.searchUsers(cleanSearch, pageable)
                    .map(this::mapToProfileResponse);
        }

        // No search query — just filter by status or return all
        if (isActiveFilter != null) {
            return userRepository.findByIsActive(isActiveFilter, pageable)
                    .map(this::mapToProfileResponse);
        }
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
                .isMobileVerified(user.getIsMobileVerified())
                .isEmailVerified(user.getIsEmailVerified())
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
