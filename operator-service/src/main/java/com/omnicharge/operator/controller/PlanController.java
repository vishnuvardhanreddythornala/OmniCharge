package com.omnicharge.operator.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.service.PlanQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanQueryService planQueryService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlanById(@PathVariable Long id) {
        PlanResponse plan = planQueryService.getPlanById(id);
        return ResponseEntity.ok(ApiResponse.success("Plan retrieved successfully", plan));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PlanResponse>>> searchPlans(
            @RequestParam Long operatorId,
            @RequestParam(required = false) PlanCategory category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<PlanResponse> plans = planQueryService.searchPlansFromRedis(operatorId, category, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(ApiResponse.success("Plans retrieved successfully", plans));
    }
}
