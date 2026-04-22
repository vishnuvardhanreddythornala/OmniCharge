package com.omnicharge.operator.controller;
import static org.mockito.ArgumentMatchers.isNull;


import com.omnicharge.operator.common.dto.ApiResponse;
import com.omnicharge.operator.dto.OperatorRequest;
import com.omnicharge.operator.dto.OperatorResponse;
import com.omnicharge.operator.dto.PlanRequest;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.service.IOperatorService;
import com.omnicharge.operator.service.IPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AdminOperatorControllerTest {

    @Mock
    private IOperatorService operatorService;

    @Mock
    private IPlanService planService;

    @InjectMocks
    private AdminOperatorController controller;

    private OperatorResponse validOperator;
    private OperatorRequest validOperatorReq;
    private PlanResponse validPlan;
    private PlanRequest validPlanReq;

    @BeforeEach
    void setUp() {
        validOperator = OperatorResponse.builder().id(1L).name("Airtel").build();
        validOperatorReq = new OperatorRequest();
        validOperatorReq.setName("Airtel");

        validPlan = PlanResponse.builder().id(10L).planName("Plan10").build();
        validPlanReq = new PlanRequest();
        validPlanReq.setPlanName("Plan10");
    }

    @Test
    void getAllOperators_Active() {
        when(operatorService.getOperatorsByStatus(true)).thenReturn(List.of(validOperator));
        ResponseEntity<ApiResponse<List<OperatorResponse>>> response = controller.getAllOperators("ACTIVE");
        assertTrue(response.getBody().isSuccess());
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getAllOperators_Inactive() {
        when(operatorService.getOperatorsByStatus(false)).thenReturn(List.of(validOperator));
        ResponseEntity<ApiResponse<List<OperatorResponse>>> response = controller.getAllOperators("INACTIVE");
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void getAllOperators_All() {
        when(operatorService.getOperatorsByStatus(null)).thenReturn(List.of(validOperator));
        ResponseEntity<ApiResponse<List<OperatorResponse>>> response = controller.getAllOperators("ALL");
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void getAllOperators_InvalidStatus() {
        when(operatorService.getOperatorsByStatus(null)).thenReturn(List.of(validOperator));
        ResponseEntity<ApiResponse<List<OperatorResponse>>> response = controller.getAllOperators("FOO");
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void createOperator() {
        when(operatorService.createOperator(any())).thenReturn(validOperator);
        ResponseEntity<ApiResponse<OperatorResponse>> response = controller.createOperator(validOperatorReq);
        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    void updateOperator() {
        when(operatorService.updateOperator(eq(1L), any())).thenReturn(validOperator);
        ResponseEntity<ApiResponse<OperatorResponse>> response = controller.updateOperator(1L, validOperatorReq);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void deleteOperator() {
        doNothing().when(operatorService).deleteOperator(1L);
        ResponseEntity<ApiResponse<Void>> response = controller.deleteOperator(1L);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void activateOperator() {
        when(operatorService.activateOperator(1L)).thenReturn(validOperator);
        ResponseEntity<ApiResponse<OperatorResponse>> response = controller.activateOperator(1L);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void deactivateOperator() {
        when(operatorService.deactivateOperator(1L)).thenReturn(validOperator);
        ResponseEntity<ApiResponse<OperatorResponse>> response = controller.deactivateOperator(1L);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getOperatorPlans() {
        when(planService.getPlansByOperatorAndStatus(1L, true)).thenReturn(List.of(validPlan));
        ResponseEntity<ApiResponse<List<PlanResponse>>> response = controller.getOperatorPlans(1L, "ACTIVE");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void createPlan() {
        when(planService.createPlan(eq(1L), any())).thenReturn(validPlan);
        ResponseEntity<ApiResponse<PlanResponse>> response = controller.createPlan(1L, validPlanReq);
        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    void updatePlan() {
        when(planService.updatePlan(eq(10L), any())).thenReturn(validPlan);
        ResponseEntity<ApiResponse<PlanResponse>> response = controller.updatePlan(10L, validPlanReq);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void deletePlan() {
        doNothing().when(planService).deletePlan(10L);
        ResponseEntity<ApiResponse<Void>> response = controller.deletePlan(10L);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void activatePlan() {
        when(planService.activatePlan(10L)).thenReturn(validPlan);
        ResponseEntity<ApiResponse<PlanResponse>> response = controller.activatePlan(10L);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void deactivatePlan() {
        when(planService.deactivatePlan(10L)).thenReturn(validPlan);
        ResponseEntity<ApiResponse<PlanResponse>> response = controller.deactivatePlan(10L);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void searchAllPlans() {
        Page<PlanResponse> page = new PageImpl<>(List.of(validPlan));
        when(planService.searchPlansWithStatus(eq(1L), eq(PlanCategory.DATA), eq(true), eq("Search"), any(PageRequest.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Page<PlanResponse>>> response = controller.searchAllPlans(
                1L, PlanCategory.DATA, "ACTIVE", "Search", 0, 10, "price", "DESC");
        
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void searchAllPlans_Asc() {
        Page<PlanResponse> page = new PageImpl<>(List.of(validPlan));
        when(planService.searchPlansWithStatus(isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Page<PlanResponse>>> response = controller.searchAllPlans(
                null, null, null, null, 0, 10, "price", "ASC");
        
        assertEquals(200, response.getStatusCode().value());
    }
}
