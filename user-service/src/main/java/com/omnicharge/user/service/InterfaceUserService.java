package com.omnicharge.user.service;

import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.dto.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InterfaceUserService {
    
    UserProfileResponse getProfile(Long userId);
    
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    // Admin methods
    Page<UserProfileResponse> getAllUsers(String search, String status, Pageable pageable);
    
    UserProfileResponse getUserById(Long id);
    
    void toggleUserStatus(Long id, boolean active);
}
