package com.omnicharge.operator.service;

import com.omnicharge.operator.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.common.logging.LogEvent;
import com.omnicharge.operator.common.logging.LogEventPublisher;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock private PlanRepository planRepository;
    @Mock private OperatorRepository operatorRepository;
    @Mock private OperatorEventPublisher operatorEventPublisher;
    @Mock private LogEventPublisher logEventPublisher;

    @InjectMocks
    private PlanService planService;

    private Operator operator;
    private Plan plan;

    @BeforeEach
    void setUp() {
        operator = new Operator();
        operator.setId(1L);
        operator.setName("Jio");
        operator.setIsActive(true);

        plan = new Plan();
        plan.setId(100L);
        plan.setOperator(operator);
        plan.setPlanName("Unlimited");
        plan.setCategory(PlanCategory.DATA);
        plan.setPrice(new BigDecimal("299"));
        plan.setIsActive(true);
    }

    @Test
    void getPlanById_Success() {
        when(planRepository.findActiveById(100L)).thenReturn(Optional.of(plan));
        PlanResponse resp = planService.getPlanById(100L);
        assertNotNull(resp);
        assertEquals("Unlimited", resp.getPlanName());
    }

    @Test
    void getPlanById_NotFound() {
        when(planRepository.findActiveById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> planService.getPlanById(1L));
    }

    @Test
    void searchPlans_Success() {
        Page<Plan> p = new PageImpl<>(List.of(plan));
        when(planRepository.searchActivePlans(1L, PlanCategory.DATA, null, null, PageRequest.of(0, 10))).thenReturn(p);

        Page<PlanResponse> result = planService.searchPlans(1L, PlanCategory.DATA, null, null, PageRequest.of(0, 10));
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createPlan_Success() {
        PlanRequest req = new PlanRequest();
        req.setPlanName("Jio 5G");
        req.setPrice(new BigDecimal("199"));

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);

        PlanResponse result = planService.createPlan(1L, req);

        assertNotNull(result);
        verify(planRepository, times(1)).save(any(Plan.class));
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void createPlan_OperatorNotFound() {
        PlanRequest req = new PlanRequest();
        when(operatorRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> planService.createPlan(1L, req));
    }

    @Test
    void updatePlan_Success() {
        PlanRequest req = new PlanRequest();
        req.setPlanName("Jio 4G");

        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);

        planService.updatePlan(100L, req);

        verify(planRepository, times(1)).save(plan);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void deletePlan_Success() {
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));

        planService.deletePlan(100L);

        assertFalse(plan.getIsActive());
        assertFalse(plan.getDeactivatedByOperator());
        verify(planRepository, times(1)).save(plan);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void activatePlan_Success() {
        plan.setIsActive(false);
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);

        planService.activatePlan(100L);

        assertTrue(plan.getIsActive());
        assertFalse(plan.getDeactivatedByOperator());
        verify(planRepository, times(1)).save(plan);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void activatePlan_OperatorInactive() {
        operator.setIsActive(false);
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));

        assertThrows(com.omnicharge.operator.common.exception.BadRequestException.class, () -> planService.activatePlan(100L));
    }

    @Test
    void deactivatePlan_Success() {
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);

        planService.deactivatePlan(100L);

        assertFalse(plan.getIsActive());
        assertFalse(plan.getDeactivatedByOperator());
        verify(planRepository, times(1)).save(plan);
    }
}
