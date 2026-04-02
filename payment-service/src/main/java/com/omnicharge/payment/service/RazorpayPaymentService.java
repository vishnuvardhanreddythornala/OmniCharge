package com.omnicharge.payment.service;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
// Production imports (restore when enabling real Razorpay checkout):
// import com.razorpay.Order;
// import com.razorpay.RazorpayClient;
// import com.razorpay.RazorpayException;
// import org.json.JSONObject;
// import java.math.BigDecimal;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RazorpayPaymentService implements IRazorpayPaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;
    
    private final LogEventPublisher logEventPublisher;

    @Override
    @CircuitBreaker(name = "razorpayService", fallbackMethod = "processPaymentFallback")
    public PaymentResponse processRazorpayPayment(PaymentRequest request) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        // --- DEVELOPMENT MODE: Simulate Razorpay SUCCESS for saga testing ---
        // Razorpay live keys require a frontend checkout step (user browser authorization).
        // A server-only orders.create() call cannot auto-capture a payment.
        // When integrating a real checkout UI, remove this block and uncomment below.
        String simulatedOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[DEV] Simulated Razorpay order: {} for recharge: {}. Waiting for manual confirmation via API...", 
                 simulatedOrderId, request.getRechargeId());
                 
        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status("PENDING")
                .razorpayOrderId(simulatedOrderId)
                .amount(request.getAmount())
                .timestamp(LocalDateTime.now())
                .build();

        /* --- PRODUCTION: Uncomment below and remove the simulation block above ---
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            long amountInPaise = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", transactionId);
            orderRequest.put("payment_capture", 1);

            Order order = razorpay.orders.create(orderRequest);
            String orderId = order.get("id");
            log.info("Razorpay Order created: {} for recharge: {}", orderId, request.getRechargeId());

            return PaymentResponse.builder()
                    .transactionId(transactionId)
                    .status("SUCCESS")
                    .razorpayOrderId(orderId)
                    .amount(request.getAmount())
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for recharge: {}", request.getRechargeId(), e);
            return PaymentResponse.builder()
                    .transactionId(transactionId)
                    .status("FAILED")
                    .razorpayOrderId(null)
                    .amount(request.getAmount())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        --- END PRODUCTION BLOCK --- */
    }

    public PaymentResponse processPaymentFallback(PaymentRequest request, Exception e) {
        log.error("Circuit breaker activated - Razorpay service unavailable for recharge: {}", request.getRechargeId(), e);

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status("FAILED")
                .razorpayOrderId(null)
                .amount(request.getAmount())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public void processRefund(String paymentId, BigDecimal amount) {
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValue());
            razorpay.payments.refund(paymentId, refundRequest);
            log.info("Refund successful for payment ID: {}", paymentId);
            
            // Log business operation: REFUND_PROCESSED
            Map<String, Object> refundContext = new HashMap<>();
            refundContext.put("paymentId", paymentId);
            refundContext.put("refundAmount", amount.toString());
            refundContext.put("status", "SUCCESS");
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("INFO")
                    .message("Refund processed successfully")
                    .eventType("REFUND_PROCESSED")
                    .context(refundContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (RazorpayException e) {
            log.error("Refund failed for payment ID: {}", paymentId, e);
            
            // Log business operation: REFUND_FAILED
            Map<String, Object> refundFailedContext = new HashMap<>();
            refundFailedContext.put("paymentId", paymentId);
            refundFailedContext.put("refundAmount", amount.toString());
            refundFailedContext.put("status", "FAILED");
            refundFailedContext.put("errorMessage", e.getMessage());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("ERROR")
                    .message("Refund processing failed")
                    .eventType("REFUND_FAILED")
                    .context(refundFailedContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
}
