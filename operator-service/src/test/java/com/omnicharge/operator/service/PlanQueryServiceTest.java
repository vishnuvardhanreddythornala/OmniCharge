package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanQueryServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private OperatorEventPublisher operatorEventPublisher;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PlanQueryService planQueryService;

    private Plan plan;
    private PlanResponse planResponse;

    @BeforeEach
    void setUp() {
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setName("Jio");

        plan = new Plan();
        plan.setId(10L);
        plan.setOperator(operator);
        plan.setPrice(new BigDecimal("299.00"));
        plan.setCategory(PlanCategory.DATA);
        plan.setIsActive(true);

        planResponse = PlanResponse.builder()
                .id(10L)
                .price(new BigDecimal("299.00"))
                .operatorId(1L)
                .build();
    }

    @Test
    void getPlanById_CacheHit() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plan:detail:10")).thenReturn("{planJson}");
        when(objectMapper.readValue("{planJson}", PlanResponse.class)).thenReturn(planResponse);

        PlanResponse result = planQueryService.getPlanById(10L);

        assertNotNull(result);
        assertEquals(new BigDecimal("299.00"), result.getPrice());
        verify(planRepository, never()).findActiveById(anyLong()); // Validates Redis intercepted the query
    }

    @Test
    void getPlanById_FallbackToDB() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plan:detail:10")).thenReturn(null); // Cache Miss
        when(planRepository.findActiveById(10L)).thenReturn(Optional.of(plan));
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        PlanResponse result = planQueryService.getPlanById(10L); // Evaluates fallback method implicitly inside the catch block conceptually

        assertNotNull(result);
        assertEquals(new BigDecimal("299.00"), result.getPrice());
        verify(planRepository, times(1)).findActiveById(10L);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void searchPlansFromRedis_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(planResponse));

        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).getId());
    }

    @Test
    void fallbackSearchPlans_DatabaseExecution() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        Page<Plan> dbPage = new PageImpl<>(List.of(plan));

        when(planRepository.searchActivePlans(1L, null, null, null, pageable)).thenReturn(dbPage);
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        Page<PlanResponse> result = planQueryService.fallbackSearchPlans(1L, null, null, null, pageable, new RuntimeException("Test Miss"));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }
}
