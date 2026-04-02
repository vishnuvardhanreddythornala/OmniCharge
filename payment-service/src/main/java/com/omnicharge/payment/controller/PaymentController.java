package com.omnicharge.payment.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.service.IPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        
        // Security: Validate that the authenticated user matches the request userId
        if (!request.getUserId().equals(authenticatedUserId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unauthorized: Cannot create payment for another user. Request userId: " 
                            + request.getUserId() + ", Header userId: " + authenticatedUserId));
        }
        
        PaymentResponse payment = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", payment));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable String transactionId,
            @RequestHeader("X-User-Id") Long userId) {
        TransactionResponse transaction = paymentService.getTransaction(transactionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully", transaction));
    }

    @PostMapping("/webhook/confirm/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> confirmPaymentManually(
            @PathVariable String transactionId,
            @RequestParam(required = false) String razorpayPaymentId,
            @RequestParam(required = false) String razorpaySignature) {
        
        // In a real app, we verify the razorpaySignature before proceeding
        // For development, we just mark the payment as SUCCESS
        TransactionResponse transaction = paymentService.confirmPayment(transactionId, razorpayPaymentId, razorpaySignature);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed successfully", transaction));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getPaymentHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TransactionResponse> transactions = paymentService.getPaymentHistory(
                userId, minAmount, maxAmount, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved successfully", transactions));
    }
}
