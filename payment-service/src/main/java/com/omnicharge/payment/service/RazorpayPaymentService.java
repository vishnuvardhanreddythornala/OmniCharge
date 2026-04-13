package com.omnicharge.payment.service;

import com.omnicharge.payment.common.logging.LogEvent;
import com.omnicharge.payment.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.razorpay.Order;
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

        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            // Convert amount from INR (rupees) to paise for Razorpay API
            long amountInPaise = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", transactionId);
            orderRequest.put("payment_capture", 1); // Auto-capture after successful authorization

            Order order = razorpay.orders.create(orderRequest);
            String orderId = order.get("id");
            
            log.info("Razorpay Order created: {} | Amount: {} paise | Receipt: {} | RechargeId: {}", 
                     orderId, amountInPaise, transactionId, request.getRechargeId());

            // Log business operation
            Map<String, Object> rzpContext = new HashMap<>();
            rzpContext.put("transactionId", transactionId);
            rzpContext.put("rechargeId", request.getRechargeId());
            rzpContext.put("razorpayOrderId", orderId);
            rzpContext.put("amountPaise", amountInPaise);
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("INFO")
                    .message("Razorpay order created successfully")
                    .eventType("RAZORPAY_ORDER_CREATED")
                    .context(rzpContext)
                    .timestamp(LocalDateTime.now())
                    .build());

            // Return PENDING — payment will be confirmed when user completes checkout in the browser
            return PaymentResponse.builder()
                    .transactionId(transactionId)
                    .status("PENDING")
                    .razorpayOrderId(orderId)
                    .amount(request.getAmount())
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation FAILED for recharge: {} | Error: {}", 
                      request.getRechargeId(), e.getMessage(), e);

            // Log business operation
            Map<String, Object> failContext = new HashMap<>();
            failContext.put("transactionId", transactionId);
            failContext.put("rechargeId", request.getRechargeId());
            failContext.put("errorMessage", e.getMessage());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("ERROR")
                    .message("Razorpay order creation failed")
                    .eventType("RAZORPAY_ORDER_FAILED")
                    .context(failContext)
                    .timestamp(LocalDateTime.now())
                    .build());

            return PaymentResponse.builder()
                    .transactionId(transactionId)
                    .status("FAILED")
                    .razorpayOrderId(null)
                    .amount(request.getAmount())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
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
