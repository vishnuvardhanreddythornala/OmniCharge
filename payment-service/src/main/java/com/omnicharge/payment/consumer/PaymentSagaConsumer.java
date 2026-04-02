package com.omnicharge.payment.consumer;

import com.omnicharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.common.event.saga.RechargeInitiatedEvent;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
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
        
        try {
            log.info("Adding contexts to PaymentRequest: email={}, mobile={}, op={}, plan={}, target={}", 
                    event.getUserEmail(), event.getUserMobile(), event.getOperatorName(), event.getPlanName(), event.getMobileNumber());
            
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setRechargeId(event.getRechargeId());
            paymentRequest.setUserId(event.getUserId());
            paymentRequest.setAmount(event.getAmount());
            paymentRequest.setPaymentMethod(event.getPaymentMethod());
            paymentRequest.setUserEmail(event.getUserEmail());
            paymentRequest.setUserMobile(event.getUserMobile());
            paymentRequest.setMobileNumber(event.getMobileNumber());
            paymentRequest.setOperatorName(event.getOperatorName());
            paymentRequest.setPlanName(event.getPlanName());

            PaymentResponse response = paymentService.processPayment(paymentRequest);
            
            if ("SUCCESS".equals(response.getStatus())) {
                PaymentApprovedEvent approvedEvent = PaymentApprovedEvent.builder()
                        .rechargeId(event.getRechargeId())
                        .transactionId(response.getTransactionId())
                        .razorpayOrderId(response.getRazorpayOrderId())
                        .amount(response.getAmount())
                        .status("SUCCESS")
                        .timestamp(LocalDateTime.now())
                        .build();
                paymentEventProducer.publishPaymentApproved(approvedEvent);
                
                // Log SAGA processing success
                Map<String, Object> successContext = new HashMap<>();
                successContext.put("rechargeId", event.getRechargeId());
                successContext.put("transactionId", response.getTransactionId());
                successContext.put("processingStatus", "APPROVED");
                
                logEventPublisher.publish(LogEvent.builder()
                        .serviceName("payment-service")
                        .level("INFO")
                        .message("SAGA processing completed: Payment approved")
                        .eventType("SAGA_PROCESSING_SUCCESS")
                        .context(successContext)
                        .timestamp(LocalDateTime.now())
                        .build());
            } else if ("PENDING".equals(response.getStatus())) {
                log.info("Payment creation PENDING for transaction: {}. Waiting for manual confirmation webhook.", 
                         response.getTransactionId());
                
                // Log SAGA processing pending
                Map<String, Object> pendingContext = new HashMap<>();
                pendingContext.put("rechargeId", event.getRechargeId());
                pendingContext.put("transactionId", response.getTransactionId());
                pendingContext.put("processingStatus", "PENDING_CONFIRMATION");
                
                logEventPublisher.publish(LogEvent.builder()
                        .serviceName("payment-service")
                        .level("INFO")
                        .message("SAGA processing pending: Awaiting webhook confirmation")
                        .eventType("SAGA_PROCESSING_PENDING")
                        .context(pendingContext)
                        .timestamp(LocalDateTime.now())
                        .build());
            } else {
                PaymentRejectedEvent rejectedEvent = PaymentRejectedEvent.builder()
                        .rechargeId(event.getRechargeId())
                        .failureReason("Payment creation failed via Razorpay")
                        .timestamp(LocalDateTime.now())
                        .build();
                paymentEventProducer.publishPaymentRejected(rejectedEvent);
                
                // Log SAGA processing failure
                Map<String, Object> failureContext = new HashMap<>();
                failureContext.put("rechargeId", event.getRechargeId());
                failureContext.put("processingStatus", "REJECTED");
                failureContext.put("failureReason", "Payment creation failed via Razorpay");
                
                logEventPublisher.publish(LogEvent.builder()
                        .serviceName("payment-service")
                        .level("WARN")
                        .message("SAGA processing failed: Payment rejected")
                        .eventType("SAGA_PROCESSING_FAILED")
                        .context(failureContext)
                        .timestamp(LocalDateTime.now())
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to process payment for rechargeId: {}", event.getRechargeId(), e);
            PaymentRejectedEvent rejectedEvent = PaymentRejectedEvent.builder()
                    .rechargeId(event.getRechargeId())
                    .failureReason(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            paymentEventProducer.publishPaymentRejected(rejectedEvent);
            
            // Log SAGA processing exception
            Map<String, Object> exceptionContext = new HashMap<>();
            exceptionContext.put("rechargeId", event.getRechargeId());
            exceptionContext.put("processingStatus", "EXCEPTION");
            exceptionContext.put("errorMessage", e.getMessage());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("ERROR")
                    .message("SAGA processing exception: " + e.getMessage())
                    .eventType("SAGA_PROCESSING_EXCEPTION")
                    .context(exceptionContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
}
