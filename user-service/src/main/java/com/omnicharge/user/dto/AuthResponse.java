package com.omnicharge.user.dto;

import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    
    @Builder.Default
    private String tokenType = "Bearer";
    
    private Long expiresIn;
    private Role role;
    private String fullName;
    private String email;
    private AuthProvider authProvider;
    private Boolean isProfileComplete;
}
