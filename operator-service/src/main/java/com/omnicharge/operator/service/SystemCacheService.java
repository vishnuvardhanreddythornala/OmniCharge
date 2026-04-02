package com.omnicharge.operator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemCacheService {

    private final PlanRepository planRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String SYSTEM_INIT_KEY = "system:cache:initialized";

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void handleApplicationReady() {
        // Cold Start Handling: Check if cache needs rebuilding
        Boolean initialized = redisTemplate.hasKey(SYSTEM_INIT_KEY);
        if (Boolean.FALSE.equals(initialized)) {
            log.info("Cold Start detected: Redis cache is empty. Initiating auto-rebuild...");
            rebuildRedisCache();
            // Setting a 24-hour expiration token to prevent constant rebuilds if the service bounces often
            redisTemplate.opsForValue().set(SYSTEM_INIT_KEY, "true", Duration.ofHours(24));
        } else {
            log.info("Redis cache is already initialized. Skipping cold start rebuild.");
        }
    }

    @Transactional(readOnly = true)
    public void rebuildRedisCache() {
        log.info("Starting full Redis cache rebuild for Operator Service...");
        
        List<Plan> allActivePlans = planRepository.findAll()
                .stream()
                .filter(Plan::getIsActive)
                .collect(Collectors.toList());

        // Group by operatorId
        Map<Long, List<Plan>> plansByOperator = allActivePlans.stream()
                .collect(Collectors.groupingBy(p -> p.getOperator().getId()));

        int count = 0;
        for (Map.Entry<Long, List<Plan>> entry : plansByOperator.entrySet()) {
            Long operatorId = entry.getKey();
            List<PlanResponse> responses = entry.getValue().stream().map(this::mapToResponse).collect(Collectors.toList());
            
            String cacheKey = "plans:operator:" + operatorId;
            try {
                String jsonPlans = objectMapper.writeValueAsString(responses);
                redisTemplate.opsForValue().set(cacheKey, jsonPlans);

                for (PlanResponse pr : responses) {
                    String detailKey = "plan:detail:" + pr.getId();
                    redisTemplate.opsForValue().set(detailKey, objectMapper.writeValueAsString(pr));
                }
                count += responses.size();
            } catch (Exception e) {
                log.error("Failed to rebuild cache for operator {}", operatorId, e);
            }
        }
        log.info("Successfully rebuilt Redis cache for {} active plans.", count);
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
