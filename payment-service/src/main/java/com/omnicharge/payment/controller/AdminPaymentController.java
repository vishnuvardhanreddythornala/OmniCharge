package com.omnicharge.payment.controller;

import com.omnicharge.payment.common.dto.ApiResponse;
import com.omnicharge.payment.dto.PaymentStatsResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.service.IPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Payments", description = "Admin-only operations for payments")
@SecurityRequirement(name = "bearerAuth")
public class AdminPaymentController {

    private final IPaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllTransactions(
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String rechargeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        // Security: Verify ADMIN role
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Access denied: Admin role required"));
        }

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TransactionResponse> transactions = paymentService.getAllTransactions(
                userId, minAmount, maxAmount, status, startDate, endDate, rechargeId, pageable);
        return ResponseEntity.ok(ApiResponse.success("All transactions retrieved successfully", transactions));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PaymentStatsResponse>> getPaymentStats(
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        
        // Security: Verify ADMIN role
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Access denied: Admin role required"));
        }

        PaymentStatsResponse stats = paymentService.getPaymentStats(days);
        return ResponseEntity.ok(ApiResponse.success("Payment stats retrieved successfully", stats));
    }
}
