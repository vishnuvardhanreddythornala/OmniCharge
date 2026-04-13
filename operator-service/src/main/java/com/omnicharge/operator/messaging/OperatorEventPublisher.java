package com.omnicharge.operator.messaging;

import com.omnicharge.operator.config.RabbitMQConfig;
import com.omnicharge.operator.event.PlanUpdatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OperatorEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPlanUpdatedEvent(Long operatorId) {
        String eventId = UUID.randomUUID().toString();
        PlanUpdatedMessage message = PlanUpdatedMessage.builder()
                .eventId(eventId)
                .operatorId(operatorId)
                .timestamp(Instant.now().toEpochMilli())
                .build();
                
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendEvent(eventId, operatorId, message);
                }
            });
        } else {
            sendEvent(eventId, operatorId, message);
        }
    }

    private void sendEvent(String eventId, Long operatorId, PlanUpdatedMessage message) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "plan.updated", message);
            log.info("Published plan.updated event {} for operatorId: {}", eventId, operatorId);
        } catch (Exception e) {
            log.error("Failed to publish plan.updated event {} for operatorId: {}", eventId, operatorId, e);
        }
    }
}
