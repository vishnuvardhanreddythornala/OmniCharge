package com.omnicharge.user.service;

public interface IEmailService {
    
    void sendOtpEmail(String toEmail, String otp);
}
