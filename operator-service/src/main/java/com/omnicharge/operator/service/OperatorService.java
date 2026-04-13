package com.omnicharge.operator.service;

import com.omnicharge.operator.common.exception.DuplicateResourceException;
import com.omnicharge.operator.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.common.logging.LogEvent;
import com.omnicharge.operator.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.OperatorRequest;
import com.omnicharge.operator.dto.OperatorResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.OperatorCategory;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
import com.omnicharge.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperatorService implements IOperatorService {

    private final OperatorRepository operatorRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final LogEventPublisher logEventPublisher;
    private final OperatorEventPublisher operatorEventPublisher;

    @Override
    public OperatorResponse getOperatorById(Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));
        return mapToResponse(operator);
    }

    @Override
    public OperatorResponse getActiveOperatorById(Long id) {
        Operator operator = operatorRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));
        return mapToResponse(operator);
    }

    @Override
    public List<OperatorResponse> getOperatorsByCategory(OperatorCategory category) {
        return operatorRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OperatorResponse> getAllOperators() {
        return operatorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OperatorResponse> getActiveOperators() {
        return operatorRepository.findByIsActive(true).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OperatorResponse> getOperatorsByStatus(Boolean isActive) {
        if (isActive == null) {
            return getAllOperators();
        }
        return operatorRepository.findByIsActive(isActive).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OperatorResponse createOperator(OperatorRequest request) {
        // Check for duplicates
        if (operatorRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Operator with code '" + request.getCode() + "' already exists");
        }
        
        if (operatorRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Operator with name '" + request.getName() + "' already exists");
        }

        Operator operator = new Operator();
        operator.setName(request.getName());
        operator.setCode(request.getCode());
        operator.setCategory(request.getCategory());
        operator.setLogoUrl(request.getLogoUrl());
        operator.setIsActive(true);

        operator = operatorRepository.save(operator);
        log.info("Created operator: {}", operator.getName());

        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("operatorId", operator.getId());
        context.put("operatorName", operator.getName());
        context.put("operatorCode", operator.getCode());
        context.put("category", operator.getCategory().name());
        publishBusinessLog("OPERATOR_CREATED",
            "Operator created: name=" + operator.getName() + ", code=" + operator.getCode(),
            context);

        // Invalidate cache
        invalidateOperatorCache();

        return mapToResponse(operator);
    }

    @Override
    @Transactional
    public OperatorResponse updateOperator(Long id, OperatorRequest request) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));

        // Check for duplicate code (excluding current operator)
        operatorRepository.findByCode(request.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Operator with code '" + request.getCode() + "' already exists");
            }
        });

        // Check for duplicate name (excluding current operator)
        operatorRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Operator with name '" + request.getName() + "' already exists");
            }
        });

        operator.setName(request.getName());
        operator.setCode(request.getCode());
        operator.setCategory(request.getCategory());
        operator.setLogoUrl(request.getLogoUrl());

        operator = operatorRepository.save(operator);
        log.info("Updated operator: {}", operator.getName());

        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("operatorId", operator.getId());
        context.put("operatorName", operator.getName());
        context.put("operatorCode", operator.getCode());
        context.put("category", operator.getCategory().name());
        publishBusinessLog("OPERATOR_UPDATED",
            "Operator updated: name=" + operator.getName() + ", code=" + operator.getCode(),
            context);

        // Invalidate cache
        invalidateOperatorCache();

        return mapToResponse(operator);
    }

    @Override
    @Transactional
    public void deleteOperator(Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));

        // Soft delete operator
        operator.setIsActive(false);
        
        // Cascade deactivation to all active plans
        for (Plan plan : operator.getPlans()) {
            if (plan.getIsActive()) {
                plan.setIsActive(false);
                plan.setDeactivatedByOperator(true); // Mark as deactivated by operator
            }
        }
        
        operatorRepository.save(operator);
        
        log.info("Soft deleted operator: {} and deactivated {} active plans", 
                operator.getName(), 
                operator.getPlans().stream().filter(p -> p.getDeactivatedByOperator()).count());

        // Invalidate cache
        invalidateOperatorCache();
        
        // Trigger Redis cache rebuild for this operator's plans
        operatorEventPublisher.publishPlanUpdatedEvent(id);
    }

    @Override
    @Transactional
    public OperatorResponse activateOperator(Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));

        // Activate operator
        operator.setIsActive(true);
        
        // Restore plans that were deactivated by operator deactivation
        int restoredCount = 0;
        for (Plan plan : operator.getPlans()) {
            if (plan.getDeactivatedByOperator()) {
                plan.setIsActive(true);
                plan.setDeactivatedByOperator(false);
                restoredCount++;
            }
        }
        
        operator = operatorRepository.save(operator);
        
        log.info("Activated operator: {} and restored {} plans", operator.getName(), restoredCount);

        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("operatorId", operator.getId());
        context.put("operatorName", operator.getName());
        context.put("operatorCode", operator.getCode());
        context.put("restoredPlansCount", restoredCount);
        publishBusinessLog("OPERATOR_ACTIVATED",
            "Operator activated: name=" + operator.getName() + ", restoredPlans=" + restoredCount,
            context);

        // Invalidate cache
        invalidateOperatorCache();
        
        // Trigger Redis cache rebuild for this operator's plans
        operatorEventPublisher.publishPlanUpdatedEvent(id);

        return mapToResponse(operator);
    }

    @Override
    @Transactional
    public OperatorResponse deactivateOperator(Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));

        // Soft delete operator
        operator.setIsActive(false);
        
        // Cascade deactivation to all active plans
        int deactivatedCount = 0;
        for (Plan plan : operator.getPlans()) {
            if (plan.getIsActive()) {
                plan.setIsActive(false);
                plan.setDeactivatedByOperator(true);
                deactivatedCount++;
            }
        }
        
        operator = operatorRepository.save(operator);
        
        log.info("Deactivated operator: {} and deactivated {} active plans", 
                operator.getName(), deactivatedCount);

        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("operatorId", operator.getId());
        context.put("operatorName", operator.getName());
        context.put("operatorCode", operator.getCode());
        context.put("deactivatedPlansCount", deactivatedCount);
        context.put("reason", "Manual deactivation");
        publishBusinessLog("OPERATOR_DEACTIVATED",
            "Operator deactivated: name=" + operator.getName() + ", deactivatedPlans=" + deactivatedCount,
            context);

        // Invalidate cache
        invalidateOperatorCache();
        
        // Trigger Redis cache rebuild for this operator's plans
        operatorEventPublisher.publishPlanUpdatedEvent(id);

        return mapToResponse(operator);
    }

    private OperatorResponse mapToResponse(Operator operator) {
        return OperatorResponse.builder()
                .id(operator.getId())
                .name(operator.getName())
                .code(operator.getCode())
                .category(operator.getCategory())
                .logoUrl(operator.getLogoUrl())
                .isActive(operator.getIsActive())
                .planCount(operator.getPlans() != null ? operator.getPlans().size() : 0)
                .lastModifiedDate(operator.getLastModifiedDate())
                .build();
    }

    private void invalidateOperatorCache() {
        // Delete all operator-related cache keys
        try {
            redisTemplate.delete("operators:active");
            log.info("Invalidated operator cache");
        } catch (Exception e) {
            log.warn("Redis is down, unable to invalidate operator cache");
        }
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
