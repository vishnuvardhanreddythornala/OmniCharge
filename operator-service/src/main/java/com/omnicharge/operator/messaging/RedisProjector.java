package com.omnicharge.operator.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.config.RabbitMQConfig;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.event.PlanUpdatedMessage;
import com.omnicharge.operator.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisProjector {

    private final PlanRepository planRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LogEventPublisher logEventPublisher;

    @RabbitListener(queues = RabbitMQConfig.PLAN_UPDATE_QUEUE)
    @Transactional(readOnly = true)
    public void consumePlanUpdatedEvent(PlanUpdatedMessage message) {
        log.info("RedisProjector: Received plan update event {}: for operatorId: {}", message.getEventId(), message.getOperatorId());
        
        // Log event received
        Map<String, Object> receiveContext = new HashMap<>();
        receiveContext.put("eventId", message.getEventId());
        receiveContext.put("operatorId", message.getOperatorId());
        receiveContext.put("eventType", "plan.updated");
        receiveContext.put("processingStatus", "RECEIVED");
        publishBusinessLog("RABBITMQ_RECEIVE",
            "Plan update event received: eventId=" + message.getEventId() + ", operatorId=" + message.getOperatorId(),
            receiveContext);
        
        // Idempotent Event Processing: Prevent duplicate execution
        String dedupKey = "event:processed:" + message.getEventId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupKey, "true", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(isNew)) {
            log.info("RedisProjector: Ignoring duplicate event {}", message.getEventId());
            
            // Log duplicate event
            Map<String, Object> dupContext = new HashMap<>();
            dupContext.put("eventId", message.getEventId());
            dupContext.put("operatorId", message.getOperatorId());
            dupContext.put("processingStatus", "DUPLICATE_IGNORED");
            publishBusinessLog("RABBITMQ_RECEIVE",
                "Duplicate plan update event ignored: eventId=" + message.getEventId(),
                dupContext);
            
            return;
        }

        Long operatorId = message.getOperatorId();
        
        try {
            List<Plan> plans = planRepository.findByOperatorIdAndIsActive(operatorId, true);
            List<PlanResponse> planResponses = plans.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            String cacheKey = "plans:operator:" + operatorId;
            String jsonPlans = objectMapper.writeValueAsString(planResponses);
            redisTemplate.opsForValue().set(cacheKey, jsonPlans);
            log.info("RedisProjector: Successfully updated read model in Redis for operatorId: {}", operatorId);

            // Also cache individual plan details for O(1) single-plan lookup
            for (PlanResponse pr : planResponses) {
                String detailKey = "plan:detail:" + pr.getId();
                redisTemplate.opsForValue().set(detailKey, objectMapper.writeValueAsString(pr));
            }
            
            // Log successful processing
            Map<String, Object> successContext = new HashMap<>();
            successContext.put("eventId", message.getEventId());
            successContext.put("operatorId", message.getOperatorId());
            successContext.put("processingStatus", "SUCCESS");
            successContext.put("plansUpdated", planResponses.size());
            publishBusinessLog("RABBITMQ_RECEIVE",
                "Plan update event processed successfully: eventId=" + message.getEventId() + ", plansUpdated=" + planResponses.size(),
                successContext);
            
        } catch (JsonProcessingException e) {
            log.error("RedisProjector: Failed to serialize plans to JSON for operatorId: {}", operatorId, e);
            
            // Log processing failure
            Map<String, Object> failContext = new HashMap<>();
            failContext.put("eventId", message.getEventId());
            failContext.put("operatorId", message.getOperatorId());
            failContext.put("processingStatus", "FAILED");
            failContext.put("errorMessage", "JSON serialization error: " + e.getMessage());
            publishBusinessLog("RABBITMQ_RECEIVE",
                "Plan update event processing failed: eventId=" + message.getEventId() + ", error=" + e.getMessage(),
                failContext);
            
        } catch (Exception e) {
            log.error("RedisProjector: Redis error updating read model for operatorId: {}", operatorId, e);
            
            // Log processing failure
            Map<String, Object> failContext = new HashMap<>();
            failContext.put("eventId", message.getEventId());
            failContext.put("operatorId", message.getOperatorId());
            failContext.put("processingStatus", "FAILED");
            failContext.put("errorMessage", "Redis error: " + e.getMessage());
            publishBusinessLog("RABBITMQ_RECEIVE",
                "Plan update event processing failed: eventId=" + message.getEventId() + ", error=" + e.getMessage(),
                failContext);
        }
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
