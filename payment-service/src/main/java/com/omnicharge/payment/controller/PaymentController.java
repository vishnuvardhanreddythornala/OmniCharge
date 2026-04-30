package com.omnicharge.payment.controller;

import com.omnicharge.payment.common.dto.ApiResponse;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.service.IPaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments", description = "Process payments and webhooks")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final IPaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        
        // Security: Validate that the authenticated user matches the request userId
        if (!request.getUserId().equals(authenticatedUserId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unauthorized: Cannot create payment for another user"));
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

    @PostMapping("/webhook/fail/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> failPayment(
            @PathVariable String transactionId,
            @RequestParam(required = false, defaultValue = "Payment cancelled by user") String reason) {
        
        TransactionResponse transaction = paymentService.failPayment(transactionId, reason);
        return ResponseEntity.ok(ApiResponse.success("Payment failure recorded", transaction));
    }

    /**
     * Server-side payment verification fallback.
     * Checks Razorpay API directly when client-side handler doesn't fire.
     */
    @PostMapping("/verify/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> verifyPayment(
            @PathVariable String transactionId) {
        TransactionResponse transaction = paymentService.verifyPayment(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Payment verification completed", transaction));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getPaymentHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String transactionId,
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
                userId, transactionId, minAmount, maxAmount, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved successfully", transactions));
    }
}
