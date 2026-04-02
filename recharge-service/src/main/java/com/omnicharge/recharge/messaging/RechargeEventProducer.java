package com.omnicharge.recharge.messaging;

import com.omnicharge.common.event.RechargeCompletedEvent;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RechargeEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final LogEventPublisher logEventPublisher;

    public void publishRechargeCompleted(RechargeCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend("omnicharge.exchange", "recharge.completed", event);
            log.info("Published recharge completed event for rechargeId: {}", event.getRechargeId());

            // Log business operation: SAGA_EVENT_PUBLISHED
            Map<String, Object> completedContext = new HashMap<>();
            completedContext.put("eventType", "RechargeCompletedEvent");
            completedContext.put("rechargeId", event.getRechargeId());
            completedContext.put("routingKey", "recharge.completed");
            completedContext.put("exchange", "omnicharge.exchange");
            completedContext.put("status", event.getStatus());
            completedContext.put("userId", event.getUserId().toString());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("recharge-service")
                    .level("INFO")
                    .message("SAGA event published: RechargeCompletedEvent")
                    .eventType("SAGA_EVENT_PUBLISHED")
                    .context(completedContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish recharge completed event", e);

            // Log error
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("eventType", "RechargeCompletedEvent");
            errorContext.put("rechargeId", event.getRechargeId());
            errorContext.put("error", e.getMessage());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("recharge-service")
                    .level("ERROR")
                    .message("Failed to publish SAGA event: RechargeCompletedEvent")
                    .eventType("SAGA_EVENT_PUBLISH_FAILED")
                    .context(errorContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    public void publishRechargeInitiated(com.omnicharge.common.event.saga.RechargeInitiatedEvent event) {
        try {
            rabbitTemplate.convertAndSend("omnicharge.exchange", "saga.recharge.initiated", event);
            log.info("Published recharge initiated event for rechargeId: {}", event.getRechargeId());

            // Log business operation: SAGA_EVENT_PUBLISHED
            Map<String, Object> initiatedContext = new HashMap<>();
            initiatedContext.put("eventType", "RechargeInitiatedEvent");
            initiatedContext.put("rechargeId", event.getRechargeId());
            initiatedContext.put("routingKey", "saga.recharge.initiated");
            initiatedContext.put("exchange", "omnicharge.exchange");
            initiatedContext.put("amount", event.getAmount().toString());
            initiatedContext.put("userId", event.getUserId().toString());
            initiatedContext.put("paymentMethod", event.getPaymentMethod());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("recharge-service")
                    .level("INFO")
                    .message("SAGA event published: RechargeInitiatedEvent")
                    .eventType("SAGA_EVENT_PUBLISHED")
                    .context(initiatedContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish recharge initiated event", e);

            // Log error
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("eventType", "RechargeInitiatedEvent");
            errorContext.put("rechargeId", event.getRechargeId());
            errorContext.put("error", e.getMessage());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("recharge-service")
                    .level("ERROR")
                    .message("Failed to publish SAGA event: RechargeInitiatedEvent")
                    .eventType("SAGA_EVENT_PUBLISH_FAILED")
                    .context(errorContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
}
