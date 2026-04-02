package com.omnicharge.recharge.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.recharge.dto.ExpiringRechargeResponse;
import com.omnicharge.recharge.service.IRechargeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.repository.RechargeRepository;
import java.util.List;

@RestController
@RequestMapping("/api/internal/recharges")
@RequiredArgsConstructor
public class InternalRechargeController {

    private final IRechargeService rechargeService;
    private final RechargeRepository rechargeRepository;

    /**
     * Internal endpoint for cross-service lookup (no auth required).
     * Used by payment-service to fetch recharge details for notification enrichment.
     */
    @GetMapping("/{rechargeId}")
    public ResponseEntity<ApiResponse<RechargeResponse>> getRechargeByIdInternal(
            @PathVariable String rechargeId) {
        var recharge = rechargeRepository.findByRechargeId(rechargeId)
                .orElse(null);
        if (recharge == null) {
            return ResponseEntity.ok(ApiResponse.error("Recharge not found"));
        }
        RechargeResponse response = RechargeResponse.builder()
                .rechargeId(recharge.getRechargeId())
                .userId(recharge.getUserId())
                .mobileNumber(recharge.getMobileNumber())
                .operatorId(recharge.getOperatorId())
                .operatorName(recharge.getOperatorName())
                .planId(recharge.getPlanId())
                .planName(recharge.getPlanName())
                .amount(recharge.getAmount())
                .status(recharge.getStatus())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Recharge details retrieved", response));
    }

    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<ExpiringRechargeResponse>>> getExpiringRecharges(
            @RequestParam(defaultValue = "5") int daysLeft) {
        List<ExpiringRechargeResponse> recharges = rechargeService.getExpiringRecharges(daysLeft);
        return ResponseEntity.ok(ApiResponse.success("Expiring recharges retrieved successfully", recharges));
    }

    @GetMapping("/expired-today")
    public ResponseEntity<ApiResponse<List<ExpiringRechargeResponse>>> getExpiredToday() {
        List<ExpiringRechargeResponse> recharges = rechargeService.getExpiredToday();
        return ResponseEntity.ok(ApiResponse.success("Expired recharges retrieved successfully", recharges));
    }

    @PutMapping("/{rechargeId}/expire")
    public ResponseEntity<ApiResponse<Void>> markAsExpired(@PathVariable String rechargeId) {
        rechargeService.markAsExpired(rechargeId);
        return ResponseEntity.ok(ApiResponse.success("Recharge marked as expired", null));
    }
}
