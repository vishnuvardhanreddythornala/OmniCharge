package com.omnicharge.recharge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiringRechargeResponse {

    private String rechargeId;
    private Long userId;
    private String userEmail;
    private String userMobile;
    private String mobileNumber;
    private String operatorName;
    private String planName;
    private BigDecimal amount;
    private LocalDate expiryDate;
}
