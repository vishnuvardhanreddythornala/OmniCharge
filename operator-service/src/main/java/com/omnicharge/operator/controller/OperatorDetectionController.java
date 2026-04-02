package com.omnicharge.operator.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.operator.dto.OperatorDetectionResponse;
import com.omnicharge.operator.dto.OperatorResponse;
import com.omnicharge.operator.service.IOperatorDetectionService;
import com.omnicharge.operator.service.IOperatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorDetectionController {

    private final IOperatorDetectionService operatorDetectionService;
    private final IOperatorService operatorService;

    @GetMapping("/detect")
    public ResponseEntity<ApiResponse<OperatorDetectionResponse>> detectOperator(
            @RequestParam String mobileNumber) {
        
        OperatorDetectionResponse response = operatorDetectionService.detectOperator(mobileNumber);
        
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.error("Could not detect operator for the given mobile number"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Operator detected successfully", response));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<OperatorResponse>>> getActiveOperators() {
        List<OperatorResponse> operators = operatorService.getActiveOperators();
        return ResponseEntity.ok(ApiResponse.success("Active operators retrieved successfully", operators));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OperatorResponse>> getOperatorById(@PathVariable Long id) {
        OperatorResponse operator = operatorService.getActiveOperatorById(id);
        return ResponseEntity.ok(ApiResponse.success("Operator retrieved successfully", operator));
    }
}
