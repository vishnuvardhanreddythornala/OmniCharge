package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.common.logging.LogEvent;
import com.omnicharge.operator.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.PlanRequest;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.repository.OperatorRepository;
import com.omnicharge.operator.repository.PlanRepository;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService implements IPlanService {

    private final PlanRepository planRepository;
    private final OperatorRepository operatorRepository;
    private final OperatorEventPublisher operatorEventPublisher;
    private final LogEventPublisher logEventPublisher;

    @Override
    public List<PlanResponse> getPlansByOperator(Long operatorId) {
        // Only returns from DB as fallback or for internal logic
        // PlanQueryService handles the read side via Redis.
        List<Plan> plans = planRepository.findByOperatorIdAndIsActive(operatorId, true);
        return plans.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PlanResponse getPlanById(Long id) {
        // User-facing: only return if both plan and operator are active
        Plan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active plan not found with id: " + id));
        return mapToResponse(plan);
    }

    @Override
    public Page<PlanResponse> searchPlans(
            Long operatorId,
            PlanCategory category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        
        // User-facing: only active plans of active operators
        Page<Plan> plans = planRepository.searchActivePlans(operatorId, category, minPrice, maxPrice, pageable);
        return plans.map(this::mapToResponse);
    }

    @Override
    public List<PlanResponse> getPlansByOperatorAndStatus(Long operatorId, Boolean isActive) {
        // Admin: get plans with status filter
        List<Plan> plans = planRepository.findByOperatorIdAndStatus(operatorId, isActive);
        return plans.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PlanResponse> searchPlansWithStatus(
            Long operatorId,
            PlanCategory category,
            Boolean isActive,
            String search,
            Pageable pageable) {
        
        // Admin: search with status and text filter
        Page<Plan> plans = planRepository.searchPlansWithStatus(operatorId, category, isActive, search, pageable);
        return plans.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public PlanResponse createPlan(Long operatorId, PlanRequest request) {
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + operatorId));

        Plan plan = new Plan();
        plan.setOperator(operator);
        plan.setPlanName(request.getPlanName());
        plan.setPrice(request.getPrice());
        plan.setValidityDays(request.getValidityDays());
        plan.setDataLimit(request.getDataLimit());
        plan.setCallBenefit(request.getCallBenefit());
        plan.setSmsBenefit(request.getSmsBenefit());
        plan.setAdditionalBenefits(request.getAdditionalBenefits());
        plan.setCategory(request.getCategory());
        plan.setIsActive(true);

        plan = planRepository.save(plan);
        log.info("Created plan: {} for operator: {}", plan.getPlanName(), operator.getName());

        // Emit CQRS Event to update read models
        operatorEventPublisher.publishPlanUpdatedEvent(operatorId);

        return mapToResponse(plan);
    }

    @Override
    @Transactional
    public PlanResponse updatePlan(Long planId, PlanRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));

        plan.setPlanName(request.getPlanName());
        plan.setPrice(request.getPrice());
        plan.setValidityDays(request.getValidityDays());
        plan.setDataLimit(request.getDataLimit());
        plan.setCallBenefit(request.getCallBenefit());
        plan.setSmsBenefit(request.getSmsBenefit());
        plan.setAdditionalBenefits(request.getAdditionalBenefits());
        plan.setCategory(request.getCategory());

        plan = planRepository.save(plan);
        log.info("Updated plan: {}", plan.getPlanName());

        // Emit CQRS Event to update read models
        operatorEventPublisher.publishPlanUpdatedEvent(plan.getOperator().getId());

        return mapToResponse(plan);
    }

    @Override
    @Transactional
    public void deletePlan(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));

        // Soft delete
        plan.setIsActive(false);
        plan.setDeactivatedByOperator(false); // Manually deactivated
        planRepository.save(plan);
        
        log.info("Soft deleted plan: {}", plan.getPlanName());

        // Emit CQRS Event to update read models
        operatorEventPublisher.publishPlanUpdatedEvent(plan.getOperator().getId());
    }

    @Override
    @Transactional
    public PlanResponse activatePlan(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));

        // Check if operator is active
        if (!plan.getOperator().getIsActive()) {
            throw new com.omnicharge.operator.common.exception.BadRequestException("Cannot activate plan: operator is inactive");
        }

        plan.setIsActive(true);
        plan.setDeactivatedByOperator(false);
        plan = planRepository.save(plan);
        
        log.info("Activated plan: {}", plan.getPlanName());

        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("planId", plan.getId());
        context.put("planName", plan.getPlanName());
        context.put("operatorId", plan.getOperator().getId());
        context.put("operatorName", plan.getOperator().getName());
        context.put("price", plan.getPrice());
        context.put("category", plan.getCategory().name());
        publishBusinessLog("PLAN_ACTIVATED",
            "Plan activated: planId=" + plan.getId() + ", name=" + plan.getPlanName() + ", operator=" + plan.getOperator().getName(),
            context);

        // Emit CQRS Event to update read models
        operatorEventPublisher.publishPlanUpdatedEvent(plan.getOperator().getId());

        return mapToResponse(plan);
    }

    @Override
    @Transactional
    public PlanResponse deactivatePlan(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));

        plan.setIsActive(false);
        plan.setDeactivatedByOperator(false); // Manually deactivated
        plan = planRepository.save(plan);
        
        log.info("Deactivated plan: {}", plan.getPlanName());

        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("planId", plan.getId());
        context.put("planName", plan.getPlanName());
        context.put("operatorId", plan.getOperator().getId());
        context.put("operatorName", plan.getOperator().getName());
        context.put("price", plan.getPrice());
        context.put("category", plan.getCategory().name());
        context.put("reason", "Manual deactivation");
        publishBusinessLog("PLAN_DEACTIVATED",
            "Plan deactivated: planId=" + plan.getId() + ", name=" + plan.getPlanName() + ", reason=Manual deactivation",
            context);

        // Emit CQRS Event to update read models
        operatorEventPublisher.publishPlanUpdatedEvent(plan.getOperator().getId());

        return mapToResponse(plan);
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
                .deactivatedByOperator(plan.getDeactivatedByOperator())
                .lastModifiedDate(plan.getLastModifiedDate())
                .lastModifiedBy(plan.getLastModifiedBy())
                .build();
    }
    
    // Helper method for business operation logging
    private void publishBusinessLog(String eventType, String message, Map<String, Object> context) {
        LogEvent logEvent = LogEvent.builder()
                .serviceName("operator-service")
                .level("INFO")
                .logger(this.getClass().getName())
                .message(message)
                .eventType(eventType)
                .context(context)
                .timestamp(LocalDateTime.now())
                .build();
        logEventPublisher.publish(logEvent);
    }

}
