package com.omnicharge.operator.service;

import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.dto.PlanRequest;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
import com.omnicharge.operator.repository.OperatorRepository;
import com.omnicharge.operator.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorEventPublisher operatorEventPublisher;

    @InjectMocks
    private PlanService planService;

    private Operator operator;
    private Plan plan;
    private PlanRequest planRequest;

    @BeforeEach
    void setUp() {
        operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");
        operator.setIsActive(true);

        plan = new Plan();
        plan.setId(10L);
        plan.setOperator(operator);
        plan.setPlanName("Unlimited 5G");
        plan.setPrice(new BigDecimal("299.00"));
        plan.setCategory(PlanCategory.UNLIMITED);
        plan.setIsActive(true);

        planRequest = new PlanRequest();
        planRequest.setPlanName("Unlimited 5G Mod");
        planRequest.setPrice(new BigDecimal("399.00"));
        planRequest.setValidityDays(28);
        planRequest.setCategory(PlanCategory.UNLIMITED);
    }

    @Test
    void createPlan_Success() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        PlanResponse result = planService.createPlan(1L, planRequest);

        assertNotNull(result);
        assertEquals(1L, result.getOperatorId());
        verify(planRepository, times(1)).save(any(Plan.class));
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void createPlan_OperatorNotFound() {
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planService.createPlan(99L, planRequest));
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void updatePlan_Success() {
        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        PlanResponse result = planService.updatePlan(10L, planRequest);

        assertNotNull(result);
        assertEquals(1L, result.getOperatorId());
        verify(planRepository, times(1)).save(plan);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void deletePlan_Success() {
        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        planService.deletePlan(10L);

        assertFalse(plan.getIsActive());
        verify(planRepository, times(1)).save(plan);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }
}
