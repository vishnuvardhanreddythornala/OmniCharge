package com.omnicharge.recharge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String email;
    private Boolean isActive;
    private Boolean isMobileVerified;
    private Boolean isEmailVerified;
    private LocalDateTime createdDate;
    private String fullName;
    private String mobileNumber;
    private String role;
}
