package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.common.exception.ResourceNotFoundException;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanQueryServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ObjectMapper objectMapper;
    @Mock private PlanRepository planRepository;
    @Mock private OperatorEventPublisher operatorEventPublisher;

    @InjectMocks
    private PlanQueryService planQueryService;

    private PlanResponse planResponse;
    private Plan plan;
    private Operator operator;

    @BeforeEach
    void setUp() {
        planResponse = PlanResponse.builder()
                .id(1L)
                .planName("Basic")
                .price(new BigDecimal("99"))
                .validityDays(28)
                .build();
                
        operator = new Operator();
        operator.setId(10L);

        plan = new Plan();
        plan.setId(1L);
        plan.setOperator(operator);
        plan.setPlanName("Basic");
    }

    @Test
    void getPlanById_CacheHit() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plan:detail:1")).thenReturn("json");
        when(objectMapper.readValue("json", PlanResponse.class)).thenReturn(planResponse);

        PlanResponse result = planQueryService.getPlanById(1L);

        assertNotNull(result);
        assertEquals("Basic", result.getPlanName());
    }

    @Test
    void getPlanById_RedisFailureTriggersFallback() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis Down"));

        assertThrows(RuntimeException.class, () -> planQueryService.getPlanById(1L));
    }

    @Test
    void fallbackGetPlanById_SuccessNotFound() {
        when(planRepository.findActiveById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planQueryService.fallbackGetPlanById(1L, new RuntimeException("err")));
    }

    @Test
    void searchPlansFromRedis_CacheHit() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:10")).thenReturn("[]");
        when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of(planResponse));

        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(10L, null, null, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchPlansFromRedis_RedisFailureTriggersFallback() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis Down"));

        assertThrows(RuntimeException.class, () -> planQueryService.searchPlansFromRedis(10L, null, null, null, PageRequest.of(0, 10)));
    }
}
