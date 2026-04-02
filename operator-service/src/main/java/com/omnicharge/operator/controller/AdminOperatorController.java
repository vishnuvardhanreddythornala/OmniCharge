package com.omnicharge.operator.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.operator.dto.OperatorRequest;
import com.omnicharge.operator.dto.OperatorResponse;
import com.omnicharge.operator.dto.PlanRequest;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.service.IOperatorService;
import com.omnicharge.operator.service.IPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/operators")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperatorController {

    private final IOperatorService operatorService;
    private final IPlanService planService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OperatorResponse>>> getAllOperators(
            @RequestParam(required = false) String status) {
        
        Boolean isActive = parseStatus(status);
        List<OperatorResponse> operators = operatorService.getOperatorsByStatus(isActive);
        return ResponseEntity.ok(ApiResponse.success("Operators retrieved successfully", operators));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OperatorResponse>> createOperator(@Valid @RequestBody OperatorRequest request) {
        OperatorResponse operator = operatorService.createOperator(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Operator created successfully", operator));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OperatorResponse>> updateOperator(
            @PathVariable Long id,
            @Valid @RequestBody OperatorRequest request) {
        OperatorResponse operator = operatorService.updateOperator(id, request);
        return ResponseEntity.ok(ApiResponse.success("Operator updated successfully", operator));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOperator(@PathVariable Long id) {
        operatorService.deleteOperator(id);
        return ResponseEntity.ok(ApiResponse.success("Operator deleted successfully", null));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<OperatorResponse>> activateOperator(@PathVariable Long id) {
        OperatorResponse operator = operatorService.activateOperator(id);
        return ResponseEntity.ok(ApiResponse.success("Operator activated successfully", operator));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<OperatorResponse>> deactivateOperator(@PathVariable Long id) {
        OperatorResponse operator = operatorService.deactivateOperator(id);
        return ResponseEntity.ok(ApiResponse.success("Operator deactivated successfully", operator));
    }

    @GetMapping("/{operatorId}/plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getOperatorPlans(
            @PathVariable Long operatorId,
            @RequestParam(required = false) String status) {
        
        Boolean isActive = parseStatus(status);
        List<PlanResponse> plans = planService.getPlansByOperatorAndStatus(operatorId, isActive);
        return ResponseEntity.ok(ApiResponse.success("Plans retrieved successfully", plans));
    }

    @PostMapping("/{operatorId}/plans")
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(
            @PathVariable Long operatorId,
            @Valid @RequestBody PlanRequest request) {
        PlanResponse plan = planService.createPlan(operatorId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Plan created successfully", plan));
    }

    @PutMapping("/plans/{planId}")
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlan(
            @PathVariable Long planId,
            @Valid @RequestBody PlanRequest request) {
        PlanResponse plan = planService.updatePlan(planId, request);
        return ResponseEntity.ok(ApiResponse.success("Plan updated successfully", plan));
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long planId) {
        planService.deletePlan(planId);
        return ResponseEntity.ok(ApiResponse.success("Plan deleted successfully", null));
    }

    @PatchMapping("/plans/{planId}/activate")
    public ResponseEntity<ApiResponse<PlanResponse>> activatePlan(@PathVariable Long planId) {
        PlanResponse plan = planService.activatePlan(planId);
        return ResponseEntity.ok(ApiResponse.success("Plan activated successfully", plan));
    }

    @PatchMapping("/plans/{planId}/deactivate")
    public ResponseEntity<ApiResponse<PlanResponse>> deactivatePlan(@PathVariable Long planId) {
        PlanResponse plan = planService.deactivatePlan(planId);
        return ResponseEntity.ok(ApiResponse.success("Plan deactivated successfully", plan));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<Page<PlanResponse>>> searchAllPlans(
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) PlanCategory category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        
        Boolean isActive = parseStatus(status);
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<PlanResponse> plans = planService.searchPlansWithStatus(operatorId, category, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success("Plans retrieved successfully", plans));
    }

    private Boolean parseStatus(String status) {
        if (status == null || status.equalsIgnoreCase("ALL")) {
            return null;
        } else if (status.equalsIgnoreCase("ACTIVE")) {
            return true;
        } else if (status.equalsIgnoreCase("INACTIVE")) {
            return false;
        }
        return null; // Default to ALL
    }
}
