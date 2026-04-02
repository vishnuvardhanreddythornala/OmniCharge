package com.omnicharge.operator.service;

import com.omnicharge.operator.dto.OperatorRequest;
import com.omnicharge.operator.dto.OperatorResponse;
import com.omnicharge.operator.entity.OperatorCategory;

import java.util.List;

public interface IOperatorService {
    
    OperatorResponse getOperatorById(Long id);
    
    // Public endpoint: Get active operator by ID (returns 404 if inactive)
    OperatorResponse getActiveOperatorById(Long id);
    
    List<OperatorResponse> getOperatorsByCategory(OperatorCategory category);
    
    List<OperatorResponse> getAllOperators();
    
    List<OperatorResponse> getActiveOperators();
    
    // Admin: Get operators by status filter
    List<OperatorResponse> getOperatorsByStatus(Boolean isActive);
    
    OperatorResponse createOperator(OperatorRequest request);
    
    OperatorResponse updateOperator(Long id, OperatorRequest request);
    
    void deleteOperator(Long id);
    
    // New: Activate operator and restore plans
    OperatorResponse activateOperator(Long id);
    
    // New: Deactivate operator and cascade to plans
    OperatorResponse deactivateOperator(Long id);
}
