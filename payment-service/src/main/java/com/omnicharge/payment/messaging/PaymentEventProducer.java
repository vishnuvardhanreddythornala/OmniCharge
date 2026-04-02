package com.omnicharge.payment.messaging;

import com.omnicharge.common.event.PaymentCompletedEvent;
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
public class PaymentEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final LogEventPublisher logEventPublisher;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend("omnicharge.exchange", "payment.completed", event);
            log.info("Published payment completed event for transactionId: {}", event.getTransactionId());
            
            // Log business operation: SAGA_EVENT_PUBLISHED
            Map<String, Object> context = new HashMap<>();
            context.put("eventType", "PaymentCompletedEvent");
            context.put("transactionId", event.getTransactionId());
            context.put("rechargeId", event.getRechargeId());
            context.put("userId", event.getUserId().toString());
            context.put("status", event.getStatus());
            context.put("routingKey", "payment.completed");
            context.put("exchange", "omnicharge.exchange");
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("INFO")
                    .message("SAGA event published: PaymentCompletedEvent")
                    .eventType("SAGA_EVENT_PUBLISHED")
                    .context(context)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish payment completed event", e);
        }
    }

    public void publishPaymentApproved(com.omnicharge.common.event.saga.PaymentApprovedEvent event) {
        try {
            rabbitTemplate.convertAndSend("omnicharge.exchange", "saga.payment.approved", event);
            log.info("Published payment approved event for rechargeId: {}", event.getRechargeId());
            
            // Log business operation: SAGA_EVENT_PUBLISHED
            Map<String, Object> context = new HashMap<>();
            context.put("eventType", "PaymentApprovedEvent");
            context.put("rechargeId", event.getRechargeId());
            context.put("transactionId", event.getTransactionId());
            context.put("status", event.getStatus());
            context.put("amount", event.getAmount().toString());
            context.put("routingKey", "saga.payment.approved");
            context.put("exchange", "omnicharge.exchange");
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("INFO")
                    .message("SAGA event published: PaymentApprovedEvent")
                    .eventType("SAGA_EVENT_PUBLISHED")
                    .context(context)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish payment approved event", e);
        }
    }

    public void publishPaymentRejected(com.omnicharge.common.event.saga.PaymentRejectedEvent event) {
        try {
            rabbitTemplate.convertAndSend("omnicharge.exchange", "saga.payment.rejected", event);
            log.info("Published payment rejected event for rechargeId: {}", event.getRechargeId());
            
            // Log business operation: SAGA_EVENT_PUBLISHED
            Map<String, Object> context = new HashMap<>();
            context.put("eventType", "PaymentRejectedEvent");
            context.put("rechargeId", event.getRechargeId());
            context.put("failureReason", event.getFailureReason());
            context.put("routingKey", "saga.payment.rejected");
            context.put("exchange", "omnicharge.exchange");
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("WARN")
                    .message("SAGA event published: PaymentRejectedEvent")
                    .eventType("SAGA_EVENT_PUBLISHED")
                    .context(context)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish payment rejected event", e);
        }
    }
}
