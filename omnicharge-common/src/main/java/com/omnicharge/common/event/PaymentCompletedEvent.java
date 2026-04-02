package com.omnicharge.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent implements Serializable {
    
    private String transactionId;
    private String rechargeId;
    private Long userId;
    private String userEmail;
    private String userMobile;
    private String mobileNumber;
    private String operatorName;
    private String planName;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private LocalDateTime timestamp;
}
