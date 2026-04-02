package com.omnicharge.recharge.dto;

import com.omnicharge.recharge.entity.RechargeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeResponse {

    private Long id;
    private String rechargeId;
    private Long userId;
    private String mobileNumber;
    private Long operatorId;
    private String operatorName;
    private Long planId;
    private String planName;
    private BigDecimal amount;
    private Integer planValidityDays;
    private LocalDate planExpiryDate;
    private RechargeStatus status;
    private String failureReason;
    private String transactionId;
    private LocalDateTime createdDate;
}
