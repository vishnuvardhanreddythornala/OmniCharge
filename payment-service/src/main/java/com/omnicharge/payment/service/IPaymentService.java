package com.omnicharge.payment.service;

import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.dto.PaymentStatsResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IPaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    TransactionResponse confirmPayment(String transactionId, String razorpayPaymentId, String razorpaySignature);

    TransactionResponse getTransaction(String transactionId, Long userId);

    Page<TransactionResponse> getPaymentHistory(
            Long userId,
            String transactionId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    Page<TransactionResponse> getAllTransactions(
            Long userId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String rechargeId,
            Pageable pageable);

    PaymentStatsResponse getPaymentStats(Integer days);

    /**
     * Verifies the payment status directly with Razorpay API.
     * Fallback for when the client-side handler callback doesn't fire (e.g. UPI on non-localhost).
     */
    TransactionResponse verifyPayment(String transactionId);

    TransactionResponse failPayment(String transactionId, String failureReason);
}
