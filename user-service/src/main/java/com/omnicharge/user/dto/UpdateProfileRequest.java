package com.omnicharge.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    // SECURITY: Mobile number removed from profile update
    // Mobile numbers can ONLY be updated via /verify-mobile endpoint
}
