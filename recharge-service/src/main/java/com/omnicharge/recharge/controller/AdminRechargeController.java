package com.omnicharge.recharge.controller;

import com.omnicharge.recharge.common.dto.ApiResponse;
import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.dto.RechargeStatsResponse;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.service.IRechargeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/recharges")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRechargeController {

    private final IRechargeService rechargeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RechargeResponse>>> getAllRecharges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String status) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RechargeResponse> recharges = rechargeService.getAllRecharges(status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("All recharges retrieved successfully", recharges));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<RechargeStatsResponse>> getRechargeStats() {
        RechargeStatsResponse stats = rechargeService.getRechargeStats();
        return ResponseEntity.ok(ApiResponse.success("Recharge stats retrieved successfully", stats));
    }
}
