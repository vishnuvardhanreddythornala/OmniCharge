package com.omnicharge.user.service;

import com.omnicharge.user.dto.ChangePasswordRequest;
import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.dto.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserService {
    
    UserProfileResponse getProfile(Long userId);
    
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
    
    void changePassword(Long userId, ChangePasswordRequest request);
    
    // Admin methods
    Page<UserProfileResponse> getAllUsers(Pageable pageable);
    
    UserProfileResponse getUserById(Long id);
    
    void toggleUserStatus(Long id, boolean active);
}
