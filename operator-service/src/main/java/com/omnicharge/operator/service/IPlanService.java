package com.omnicharge.operator.service;

import com.omnicharge.operator.dto.PlanRequest;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.PlanCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface IPlanService {
    
    // User-facing: only active plans of active operators
    List<PlanResponse> getPlansByOperator(Long operatorId);
    
    PlanResponse getPlanById(Long id);
    
    Page<PlanResponse> searchPlans(
            Long operatorId,
            PlanCategory category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );
    
    // Admin: Get plans by operator with status filter
    List<PlanResponse> getPlansByOperatorAndStatus(Long operatorId, Boolean isActive);
    
    // Admin: Search plans with status filter
    Page<PlanResponse> searchPlansWithStatus(
            Long operatorId,
            PlanCategory category,
            Boolean isActive,
            Pageable pageable
    );
    
    PlanResponse createPlan(Long operatorId, PlanRequest request);
    
    PlanResponse updatePlan(Long planId, PlanRequest request);
    
    void deletePlan(Long planId);
    
    // New: Activate plan
    PlanResponse activatePlan(Long planId);
    
    // New: Deactivate plan
    PlanResponse deactivatePlan(Long planId);
}
