package com.omnicharge.payment.consumer;

import com.omnicharge.payment.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.payment.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.payment.common.event.saga.RechargeInitiatedEvent;
import com.omnicharge.payment.common.logging.LogEvent;
import com.omnicharge.payment.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.service.IPaymentService;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaConsumer {

    private final IPaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;
    private final LogEventPublisher logEventPublisher;

    @RabbitListener(queues = "saga.payment.process")
    public void consumeRechargeInitiatedEvent(RechargeInitiatedEvent event) {
        log.info("Saga Orchestration: Consumed RechargeInitiatedEvent for rechargeId: {}", event.getRechargeId());
        
        // Log business operation: SAGA_EVENT_CONSUMED
        Map<String, Object> consumedContext = new HashMap<>();
        consumedContext.put("eventType", "RechargeInitiatedEvent");
        consumedContext.put("rechargeId", event.getRechargeId());
        consumedContext.put("userId", event.getUserId().toString());
        consumedContext.put("amount", event.getAmount().toString());
        consumedContext.put("paymentMethod", event.getPaymentMethod());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("payment-service")
                .level("INFO")
                .message("SAGA event consumed: RechargeInitiatedEvent")
                .eventType("SAGA_EVENT_CONSUMED")
                .context(consumedContext)
                .timestamp(LocalDateTime.now())
                .build());
        
        // =========================================================================
        // IMPORTANT ARCHITECTURAL NOTE:
        // =========================================================================
        // We intentionally do NOT process the payment here via paymentService.processPayment().
        // The UI acts as the orchestrator for the initialization phase:
        // 1. UI calls /api/recharges/initiate -> creates Recharge
        // 2. UI calls /api/payments/process -> creates Transaction & Razorpay Order
        // 
        // If we process the payment asynchronously here as well, it causes a race condition
        // where TWO database transactions and TWO Razorpay orders are created for a single
        // recharge attempt. The SAGA is only responsible for the downstream flow 
        // (Payment Confirmation -> Update Recharge -> Send Notification).
        // =========================================================================
        
        log.info("Ignored payment creation in SAGA to prevent duplicates. Waiting for UI to call /process.");
    }
}
