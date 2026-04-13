package com.omnicharge.notification.common.event.saga;

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
public class RechargeInitiatedEvent implements Serializable {
    private String rechargeId;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String mobileNumber;
    private String operatorName;
    private String planName;
    private String userEmail;
    private String userMobile;
    private LocalDateTime timestamp;
}
