package com.omnicharge.operator.service;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.contains;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class SystemCacheServiceTest {

    @Mock private PlanRepository planRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private SystemCacheService systemCacheService;

    private Plan createActivePlan(Long id, Operator operator) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setPlanName("Plan " + id);
        plan.setPrice(new BigDecimal("199"));
        plan.setValidityDays(28);
        plan.setIsActive(true);
        plan.setOperator(operator);
        return plan;
    }

    private Operator createActiveOperator(Long id) {
        Operator op = new Operator();
        op.setId(id);
        op.setName("Operator " + id);
        op.setIsActive(true);
        return op;
    }

    @Test
    void handleApplicationReady_ColdStart()  {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(planRepository.findAll()).thenReturn(List.of());

        systemCacheService.handleApplicationReady();

        verify(redisTemplate, atLeastOnce()).opsForValue();
    }

    @Test
    void handleApplicationReady_AlreadyInitialized()  {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        systemCacheService.handleApplicationReady();

        verify(planRepository, never()).findAll();
    }

    @Test
    void rebuildRedisCache_Success() throws Exception {
        Operator op = createActiveOperator(1L);
        Plan plan = createActivePlan(1L, op);

        when(planRepository.findAll()).thenReturn(List.of(plan));
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("key1"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        systemCacheService.rebuildRedisCache();

        verify(redisTemplate, atLeastOnce()).delete(anyCollection());
        verify(valueOperations, atLeastOnce()).set(anyString(), anyString());
    }

    @Test
    void rebuildRedisCache_NoKeysToDelete() {
        when(planRepository.findAll()).thenReturn(List.of());
        when(redisTemplate.keys(anyString())).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        systemCacheService.rebuildRedisCache();

        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    void rebuildRedisCache_InactiveOperatorFiltered() {
        Operator inactiveOp = createActiveOperator(1L);
        inactiveOp.setIsActive(false);
        Plan plan = createActivePlan(1L, inactiveOp);

        when(planRepository.findAll()).thenReturn(List.of(plan));
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        systemCacheService.rebuildRedisCache();

        verify(valueOperations, never()).set(contains("plans:operator:"), anyString());
    }
}
