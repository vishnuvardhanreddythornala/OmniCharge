package com.omnicharge.payment.dto;

import com.omnicharge.payment.entity.PaymentMethod;
import com.omnicharge.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private String transactionId;
    private String rechargeId;
    private Long userId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String failureReason;
    private String razorpayOrderId;
    private String userEmail;
    private String userMobile;
    private String mobileNumber;
    private String operatorName;
    private String planName;
    private LocalDateTime createdDate;
}
