package com.omnicharge.logging.common.event.saga;

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
public class PaymentApprovedEvent implements Serializable {
    private String rechargeId;
    private String transactionId;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timestamp;
}
