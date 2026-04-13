package com.omnicharge.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpEvent implements Serializable {
    private String mobileNumber;
    private String otp;
    private Long userId;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
