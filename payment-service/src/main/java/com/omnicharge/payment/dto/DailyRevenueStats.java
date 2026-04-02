package com.omnicharge.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenueStats {
    private String date;
    private Long transactionCount;
    private BigDecimal revenue;
}
