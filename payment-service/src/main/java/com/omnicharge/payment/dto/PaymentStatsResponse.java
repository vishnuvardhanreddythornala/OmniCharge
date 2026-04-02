package com.omnicharge.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsResponse {

    private Long totalTransactions;
    private Long successfulTransactions;
    private Long failedTransactions;
    private Long pendingTransactions;
    
    private BigDecimal totalRevenue;
    private BigDecimal successAmount;
    private BigDecimal failedAmount;
    private BigDecimal averageTransactionAmount;
    
    private Long todayTransactions;
    private BigDecimal todayRevenue;
    
    private List<DailyRevenueStats> revenueByDate;
    private List<TopUserStats> topUsers;
}
