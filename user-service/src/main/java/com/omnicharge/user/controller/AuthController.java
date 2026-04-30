package com.omnicharge.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.omnicharge.user.common.dto.ApiResponse;
import com.omnicharge.user.dto.*;
import com.omnicharge.user.service.InterfaceAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and Authorization endpoints")
public class AuthController {

    private final InterfaceAuthService authService;
    private final com.omnicharge.user.service.EmailVerificationService emailVerificationService;
    private final com.omnicharge.user.util.JwtUtil jwtUtil;

    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginInitResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AdminLoginInitResponse response = authService.login(request, extractIpAddress(servletRequest));
        return ResponseEntity.ok(ApiResponse.success("Admin credentials verified. 2FA required.", response));
    }

    @PostMapping("/admin/verify-2fa")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyAdmin2fa(@Valid @RequestBody VerifyEmailOtpRequest request, HttpServletRequest servletRequest) {
        AuthResponse response = authService.verifyAdmin2fa(request, extractIpAddress(servletRequest));
        return ResponseEntity.ok(ApiResponse.success("Admin 2FA successful", response));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request, HttpServletRequest servletRequest) {
        AuthResponse response = authService.authenticateWithGoogle(request, extractIpAddress(servletRequest));
        return ResponseEntity.ok(ApiResponse.success("Google authentication successful", response));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/mobile/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendMobileOtp(
            @Valid @RequestBody SendMobileOtpRequest request,
            HttpServletRequest servletRequest) {
        authService.sendMobileOtp(request, extractIpAddress(servletRequest));
        return ResponseEntity.ok(ApiResponse.success("Mobile OTP sent successfully", null));
    }

    @PostMapping("/mobile/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMobileOtp(
            @Valid @RequestBody VerifyMobileOtpRequest request) {
        AuthResponse response = authService.verifyMobileOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Mobile authentication successful", response));
    }

    @PostMapping("/email/send-login-otp")
    public ResponseEntity<ApiResponse<Void>> sendEmailLoginOtp(
            @Valid @RequestBody SendEmailOtpRequest request,
            HttpServletRequest servletRequest) {
        authService.sendEmailOtp(request, extractIpAddress(servletRequest));
        return ResponseEntity.ok(ApiResponse.success("If account exists, an OTP has been sent via Email.", null));
    }

    @PostMapping("/email/verify-login-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmailLoginOtp(
            @Valid @RequestBody VerifyEmailOtpRequest request) {
        AuthResponse response = authService.verifyEmailOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email authentication successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        authService.logout(token);
        if (refreshTokenRequest != null && refreshTokenRequest.getRefreshToken() != null) {
            authService.logoutByRefreshToken(refreshTokenRequest.getRefreshToken());
        }
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/email/send-verification")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerification(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String email) {
        String token = authHeader.substring(7);
        String userIdStr = jwtUtil.validateToken(token).get("userId", String.class);
        Long userId = Long.valueOf(userIdStr);
        emailVerificationService.sendVerificationOtp(userId, email);
        return ResponseEntity.ok(ApiResponse.success("Verification OTP sent to " + email, null));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String otp) {
        String token = authHeader.substring(7);
        String userIdStr = jwtUtil.validateToken(token).get("userId", String.class);
        Long userId = Long.valueOf(userIdStr);
        emailVerificationService.verifyEmail(userId, otp);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }
}
