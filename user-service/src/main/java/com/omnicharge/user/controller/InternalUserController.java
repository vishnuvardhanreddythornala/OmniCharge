package com.omnicharge.user.controller;

import com.omnicharge.user.common.dto.ApiResponse;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API for inter-service communication
 * These endpoints are called by other microservices (recharge-service, payment-service, etc.)
 * No authentication required as they're behind API Gateway
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final IUserService userService;

    /**
     * Get user profile by ID
     * Used by recharge-service to fetch user email and mobile for notifications
     * 
     * @param id User ID
     * @return User profile with email and mobile number
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(@PathVariable Long id) {
        UserProfileResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    /**
     * Internal endpoint for inter-service communication (no auth required)
     * Called by recharge-service, payment-service via Feign (bypasses gateway JWT)
     */
    @GetMapping("/internal/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByIdInternal(@PathVariable Long id) {
        UserProfileResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }
}
