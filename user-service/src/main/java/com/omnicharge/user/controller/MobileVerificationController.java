package com.omnicharge.user.controller;

import com.omnicharge.user.common.dto.ApiResponse;
import com.omnicharge.user.dto.AuthResponse;
import com.omnicharge.user.dto.SendMobileOtpRequest;
import com.omnicharge.user.dto.VerifyMobileOtpRequest;
import com.omnicharge.user.service.MobileVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/mobile-otp")
@RequiredArgsConstructor
public class MobileVerificationController {

    private final MobileVerificationService mobileVerificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SendMobileOtpRequest request) {
        mobileVerificationService.sendOtp(userId, request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully to mobile number", null));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody VerifyMobileOtpRequest request) {
        AuthResponse response = mobileVerificationService.verifyOtp(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Mobile number verified successfully", response));
    }
}
