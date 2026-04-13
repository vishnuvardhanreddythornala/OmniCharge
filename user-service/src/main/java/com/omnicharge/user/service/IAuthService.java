package com.omnicharge.user.service;

import com.omnicharge.user.dto.*;

public interface IAuthService {
    
    AdminLoginInitResponse login(LoginRequest request, String ipAddress);

    AuthResponse verifyAdmin2fa(VerifyEmailOtpRequest request, String ipAddress);
    
    AuthResponse authenticateWithGoogle(GoogleAuthRequest request, String ipAddress);
    
    AuthResponse refreshToken(RefreshTokenRequest request);
    
    void sendMobileOtp(SendMobileOtpRequest request, String ipAddress);

    AuthResponse verifyMobileOtp(VerifyMobileOtpRequest request);

    void sendEmailOtp(SendEmailOtpRequest request, String ipAddress);

    AuthResponse verifyEmailOtp(VerifyEmailOtpRequest request);
    
    void logout(String token);

    void logoutByRefreshToken(String refreshToken);
}
