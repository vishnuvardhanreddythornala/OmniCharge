package com.omnicharge.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanExpiryEvent implements Serializable {
    
    private String rechargeId;
    private Long userId;
    private String userEmail;
    private String userMobile;
    private String mobileNumber;
    private String operatorName;
    private String planName;
    private BigDecimal amount;
    private LocalDate expiryDate;
    private String category; // "REMINDER" or "EXPIRED"
    private LocalDateTime timestamp;
}
