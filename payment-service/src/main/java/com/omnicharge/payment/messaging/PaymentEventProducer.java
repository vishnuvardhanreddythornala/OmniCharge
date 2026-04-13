package com.omnicharge.payment.messaging;

import com.omnicharge.payment.common.event.PaymentCompletedEvent;
import com.omnicharge.payment.common.logging.LogEvent;
import com.omnicharge.payment.common.logging.LogEventPublisher;
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
    private static final String SUCCESS_LOG = "[Payment Producer] Sending Payment Success Event for Transaction: {}";
    private static final String FAILED_LOG = "[Payment Producer] Sending Payment Failed Event for Transaction: {}";

    private static final String EXCHANGE = "omnicharge.exchange";
    private static final String SERVICE_NAME = "payment-service";
    private static final String EVENT_TYPE_SAGA = "SAGA_EVENT_PUBLISHED";

    private final RabbitTemplate rabbitTemplate;
    private final LogEventPublisher logEventPublisher;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "payment.completed", event);
            log.info("Published payment completed event for transactionId: {}", event.getTransactionId());
            
            // Log business operation: SAGA_EVENT_PUBLISHED
            Map<String, Object> context = new HashMap<>();
            context.put("eventType", "PaymentCompletedEvent");
            context.put("transactionId", event.getTransactionId());
            context.put("rechargeId", event.getRechargeId());
            context.put("userId", event.getUserId().toString());
            context.put("status", event.getStatus());
            context.put("routingKey", "payment.completed");
            context.put("exchange", EXCHANGE);
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName(SERVICE_NAME)
                    .level("INFO")
                    .message("SAGA event published: PaymentCompletedEvent")
                    .eventType(EVENT_TYPE_SAGA)
                    .context(context)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish payment completed event", e);
        }
    }

    public void publishPaymentApproved(com.omnicharge.payment.common.event.saga.PaymentApprovedEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "saga.payment.approved", event);
            log.info("Published payment approved event for rechargeId: {}", event.getRechargeId());
            
            // Log business operation: SAGA_EVENT_PUBLISHED
            Map<String, Object> context = new HashMap<>();
            context.put("eventType", "PaymentApprovedEvent");
            context.put("rechargeId", event.getRechargeId());
            context.put("transactionId", event.getTransactionId());
            context.put("status", event.getStatus());
            context.put("amount", event.getAmount().toString());
            context.put("routingKey", "saga.payment.approved");
            context.put("exchange", EXCHANGE);
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName(SERVICE_NAME)
                    .level("INFO")
                    .message("SAGA event published: PaymentApprovedEvent")
                    .eventType(EVENT_TYPE_SAGA)
                    .context(context)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish payment approved event", e);
        }
    }

    public void publishPaymentRejected(com.omnicharge.payment.common.event.saga.PaymentRejectedEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "saga.payment.rejected", event);
            log.info("Published payment rejected event for rechargeId: {}", event.getRechargeId());
            
            // Log business operation: SAGA_EVENT_PUBLISHED
            Map<String, Object> context = new HashMap<>();
            context.put("eventType", "PaymentRejectedEvent");
            context.put("rechargeId", event.getRechargeId());
            context.put("failureReason", event.getFailureReason());
            context.put("routingKey", "saga.payment.rejected");
            context.put("exchange", EXCHANGE);
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName(SERVICE_NAME)
                    .level("WARN")
                    .message("SAGA event published: PaymentRejectedEvent")
                    .eventType(EVENT_TYPE_SAGA)
                    .context(context)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish payment rejected event", e);
        }
    }
}

