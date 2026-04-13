package com.omnicharge.user.service;
import com.omnicharge.user.dto.ForgotPasswordRequest;
public interface IPasswordResetService {
    void forgotPassword(ForgotPasswordRequest request);
}
