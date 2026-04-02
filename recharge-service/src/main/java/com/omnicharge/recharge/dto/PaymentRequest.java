package com.omnicharge.recharge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private String rechargeId;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String userEmail;
    private String userMobile;
}
