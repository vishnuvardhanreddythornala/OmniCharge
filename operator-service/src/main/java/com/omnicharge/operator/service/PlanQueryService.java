package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.repository.PlanRepository;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanQueryService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PlanRepository planRepository;
    private final OperatorEventPublisher operatorEventPublisher;

    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackGetPlanById")
    public PlanResponse getPlanById(Long id) {
        String detailKey = "plan:detail:" + id;
        try {
            String jsonPlan = redisTemplate.opsForValue().get(detailKey);
            if (jsonPlan != null) {
                return objectMapper.readValue(jsonPlan, PlanResponse.class);
            }
        } catch (Exception e) {
            log.error("Redis error fetching plan id: {}", id, e);
            throw new RuntimeException("Redis unavailable", e);
        }
        // Cache miss
        return fallbackGetPlanById(id, new RuntimeException("Cache miss"));
    }

    public PlanResponse fallbackGetPlanById(Long id, Throwable t) {
        log.warn("Fallback for getPlanById({}, reason: {})", id, t.getMessage());
        Plan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active plan not found with id: " + id));
        
        // Trigger async rehydration
        operatorEventPublisher.publishPlanUpdatedEvent(plan.getOperator().getId());
        return mapToResponse(plan);
    }

    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackSearchPlans")
    public Page<PlanResponse> searchPlansFromRedis(Long operatorId, PlanCategory category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        String cacheKey = "plans:operator:" + operatorId;
        List<PlanResponse> allPlans = Collections.emptyList();
        
        try {
            String jsonPlans = redisTemplate.opsForValue().get(cacheKey);
            if (jsonPlans != null) {
                allPlans = objectMapper.readValue(jsonPlans, new TypeReference<List<PlanResponse>>() {});
            } else {
                return fallbackSearchPlans(operatorId, category, minPrice, maxPrice, pageable, new RuntimeException("Cache miss"));
            }
        } catch (Exception e) {
            log.error("Redis error fetching plans for operator: {}", operatorId, e);
            throw new RuntimeException("Redis unavailable", e);
        }

        // Apply filtering in memory
        List<PlanResponse> filtered = allPlans.stream()
            .filter(p -> category == null || p.getCategory() == category)
            .filter(p -> minPrice == null || p.getPrice().compareTo(minPrice) >= 0)
            .filter(p -> maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0)
            .collect(Collectors.toList());

        // Sort in memory
        filtered.sort((p1, p2) -> {
            boolean asc = pageable.getSort().iterator().next().isAscending();
            String prop = pageable.getSort().iterator().next().getProperty();
            int cmp = 0;
            if ("price".equals(prop)) {
                cmp = p1.getPrice().compareTo(p2.getPrice());
            } else if ("validityDays".equals(prop)) {
                cmp = p1.getValidityDays().compareTo(p2.getValidityDays());
            }
            return asc ? cmp : -cmp;
        });

        // Paginate
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        
        List<PlanResponse> paged;
        if (start > filtered.size()) {
            paged = Collections.emptyList();
        } else {
            paged = filtered.subList(start, end);
        }

        return new PageImpl<>(paged, pageable, filtered.size());
    }

    public Page<PlanResponse> fallbackSearchPlans(Long operatorId, PlanCategory category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable, Throwable t) {
        log.warn("Fallback for searchPlansFromRedis({}, reason: {})", operatorId, t.getMessage());
        
        // Trigger async rehydration
        operatorEventPublisher.publishPlanUpdatedEvent(operatorId);
        
        Page<Plan> plans = planRepository.searchActivePlans(operatorId, category, minPrice, maxPrice, pageable);
        return plans.map(this::mapToResponse);
    }

    private PlanResponse mapToResponse(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .operatorId(plan.getOperator().getId())
                .operatorName(plan.getOperator().getName())
                .planName(plan.getPlanName())
                .price(plan.getPrice())
                .validityDays(plan.getValidityDays())
                .dataLimit(plan.getDataLimit())
                .callBenefit(plan.getCallBenefit())
                .smsBenefit(plan.getSmsBenefit())
                .additionalBenefits(plan.getAdditionalBenefits())
                .category(plan.getCategory())
                .isActive(plan.getIsActive())
                .build();
    }
}
