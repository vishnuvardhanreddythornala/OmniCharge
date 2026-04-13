package com.omnicharge.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyMobileOtpRequest {
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+\\d{1,3}\\d{6,14}$", message = "Invalid mobile number")
    private String mobileNumber;

    @NotBlank(message = "OTP is required")
    private String otp;
}
