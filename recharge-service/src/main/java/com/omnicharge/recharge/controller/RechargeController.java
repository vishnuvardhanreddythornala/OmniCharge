package com.omnicharge.recharge.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.recharge.dto.RechargeRequest;
import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.service.IRechargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recharges")
@RequiredArgsConstructor
public class RechargeController {

    private final IRechargeService rechargeService;

    @PostMapping
    public ResponseEntity<ApiResponse<RechargeResponse>> initiateRecharge(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RechargeRequest request) {
        RechargeResponse recharge = rechargeService.initiateRecharge(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recharge initiated successfully", recharge));
    }

    @GetMapping("/{rechargeId}")
    public ResponseEntity<ApiResponse<RechargeResponse>> getRechargeById(
            @PathVariable String rechargeId,
            @RequestHeader("X-User-Id") Long userId) {
        RechargeResponse recharge = rechargeService.getRechargeById(rechargeId, userId);
        return ResponseEntity.ok(ApiResponse.success("Recharge retrieved successfully", recharge));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<RechargeResponse>>> getRechargeHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RechargeResponse> recharges = rechargeService.getRechargeHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Recharge history retrieved successfully", recharges));
    }

    @GetMapping("/status/{rechargeId}")
    public ResponseEntity<ApiResponse<String>> getRechargeStatus(@PathVariable String rechargeId) {
        String status = rechargeService.getRechargeStatus(rechargeId);
        return ResponseEntity.ok(ApiResponse.success("Recharge status retrieved successfully", status));
    }
}
