package com.omnicharge.user.service;

import com.omnicharge.user.dto.ForgotPasswordRequest;
import com.omnicharge.user.dto.ResetPasswordRequest;
import com.omnicharge.user.dto.VerifyOtpRequest;

public interface IPasswordResetService {
    
    void forgotPassword(ForgotPasswordRequest request);
    
    boolean verifyOtp(VerifyOtpRequest request);
    
    void resetPassword(ResetPasswordRequest request);
}
