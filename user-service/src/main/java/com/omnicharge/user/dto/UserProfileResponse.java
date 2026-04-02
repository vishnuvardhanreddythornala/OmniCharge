package com.omnicharge.user.dto;

import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
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
    private String fullName;
    private String mobileNumber;
    private Role role;
    private AuthProvider authProvider;
    private Boolean isActive;
    private LocalDateTime createdDate;
}
