package com.omnicharge.payment.service;

import com.omnicharge.common.event.PaymentCompletedEvent;
import com.omnicharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.entity.PaymentMethod;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based test for payment-service business operation logging.
 * 
 * Validates Property 33: Business Operation Event Logging
 * "For any critical business operation (user registration, recharge initiation,
 * payment processing, notification sending, plan activation/deactivation), the system
 * should log the event with relevant business context including entity IDs, amounts,
 * types, and statuses."
 * 
 * This test verifies that all critical business operations in payment-service
 * (payment processing, gateway interactions, status changes, refunds, SAGA events)
 * publish log events with appropriate business context.
 * 
 * Feature: production-grade-centralized-logging, Property 33: Business Operation Event Logging
 * Validates: Requirements 15.3
 */
@ExtendWith(MockitoExtension.class)
@Tag("property-test")
public class PaymentServiceBusinessOperationPropertyTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IRazorpayPaymentService razorpayPaymentService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
    }

    /**
     * Property: For any payment processing start, the system logs PAYMENT_PROCESSING_START event
     * with transactionId, rechargeId, userId, amount, and paymentMethod.
     */
    @Test
    void property_paymentProcessingStart_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random payment data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));
            String[] paymentMethods = {"CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING"};
            String paymentMethod = paymentMethods[random.nextInt(paymentMethods.length)];

            PaymentRequest request = new PaymentRequest();
            request.setRechargeId(rechargeId);
            request.setUserId(userId);
            request.setAmount(amount);
            request.setPaymentMethod(paymentMethod);
            request.setUserEmail("user" + userId + "@example.com");
            request.setUserMobile("9" + String.format("%09d", random.nextInt(1000000000)));
            request.setMobileNumber("9" + String.format("%09d", random.nextInt(1000000000)));
            request.setOperatorName("Operator-" + random.nextInt(10));
            request.setPlanName("Plan-" + random.nextInt(100));

            Transaction savedTransaction = new Transaction();
            savedTransaction.setId(random.nextLong(1, 100000));
            savedTransaction.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            savedTransaction.setRechargeId(rechargeId);
            savedTransaction.setUserId(userId);
            savedTransaction.setAmount(amount);
            savedTransaction.setPaymentMethod(PaymentMethod.valueOf(paymentMethod));
            savedTransaction.setStatus(PaymentStatus.SUCCESS);
            savedTransaction.setRazorpayOrderId("order_" + UUID.randomUUID().toString().substring(0, 16));

            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

            PaymentResponse mockResponse = PaymentResponse.builder()
                    .status("SUCCESS")
                    .razorpayOrderId(savedTransaction.getRazorpayOrderId())
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

            // Execute
            paymentService.processPayment(request);

            // Verify: Should publish PAYMENT_PROCESSING_START log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(1)).publish(logEventCaptor.capture());

            LogEvent startEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "PAYMENT_PROCESSING_START".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(startEvent).isNotNull();
            assertThat(startEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(startEvent.getLevel()).isEqualTo("INFO");
            assertThat(startEvent.getEventType()).isEqualTo("PAYMENT_PROCESSING_START");
            assertThat(startEvent.getContext()).containsKeys("transactionId", "rechargeId", "userId", "amount", "paymentMethod");
            assertThat(startEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(startEvent.getContext().get("userId")).isEqualTo(userId.toString());
            assertThat(startEvent.getContext().get("amount")).isEqualTo(amount.toString());
            assertThat(startEvent.getContext().get("paymentMethod")).isEqualTo(paymentMethod);

            // Reset mocks for next iteration
            reset(transactionRepository, razorpayPaymentService, paymentEventProducer, logEventPublisher);
        }
    }

    /**
     * Property: For any Razorpay gateway interaction, the system logs RAZORPAY_GATEWAY_INTERACTION event
     * with transactionId, rechargeId, razorpayOrderId, responseStatus, and amount.
     */
    @Test
    void property_razorpayGatewayInteraction_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random payment data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));
            String razorpayOrderId = "order_" + UUID.randomUUID().toString().substring(0, 16);

            PaymentRequest request = new PaymentRequest();
            request.setRechargeId(rechargeId);
            request.setUserId(userId);
            request.setAmount(amount);
            request.setPaymentMethod("UPI");
            request.setUserEmail("user" + userId + "@example.com");
            request.setUserMobile("9876543210");
            request.setMobileNumber("9876543210");
            request.setOperatorName("Jio");
            request.setPlanName("Ultimate");

            Transaction savedTransaction = new Transaction();
            savedTransaction.setId(random.nextLong(1, 100000));
            savedTransaction.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            savedTransaction.setRechargeId(rechargeId);
            savedTransaction.setUserId(userId);
            savedTransaction.setAmount(amount);
            savedTransaction.setPaymentMethod(PaymentMethod.UPI);
            savedTransaction.setStatus(PaymentStatus.SUCCESS);
            savedTransaction.setRazorpayOrderId(razorpayOrderId);

            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

            PaymentResponse mockResponse = PaymentResponse.builder()
                    .status("SUCCESS")
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

            // Execute
            paymentService.processPayment(request);

            // Verify: Should publish RAZORPAY_GATEWAY_INTERACTION log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(1)).publish(logEventCaptor.capture());

            LogEvent gatewayEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "RAZORPAY_GATEWAY_INTERACTION".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(gatewayEvent).isNotNull();
            assertThat(gatewayEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(gatewayEvent.getLevel()).isEqualTo("INFO");
            assertThat(gatewayEvent.getEventType()).isEqualTo("RAZORPAY_GATEWAY_INTERACTION");
            assertThat(gatewayEvent.getContext()).containsKeys("transactionId", "rechargeId", "razorpayOrderId", "responseStatus", "amount");
            assertThat(gatewayEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(gatewayEvent.getContext().get("razorpayOrderId")).isEqualTo(razorpayOrderId);
            assertThat(gatewayEvent.getContext().get("responseStatus")).isEqualTo("SUCCESS");
            assertThat(gatewayEvent.getContext().get("amount")).isEqualTo(amount.toString());

            // Reset mocks for next iteration
            reset(transactionRepository, razorpayPaymentService, paymentEventProducer, logEventPublisher);
        }
    }


    /**
     * Property: For any successful payment, the system logs PAYMENT_SUCCESS event
     * with transactionId, rechargeId, userId, amount, razorpayOrderId, and paymentMethod.
     */
    @Test
    void property_paymentSuccess_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random payment data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));
            String razorpayOrderId = "order_" + UUID.randomUUID().toString().substring(0, 16);

            PaymentRequest request = new PaymentRequest();
            request.setRechargeId(rechargeId);
            request.setUserId(userId);
            request.setAmount(amount);
            request.setPaymentMethod("CREDIT_CARD");
            request.setUserEmail("user" + userId + "@example.com");
            request.setUserMobile("9876543210");
            request.setMobileNumber("9876543210");
            request.setOperatorName("Airtel");
            request.setPlanName("Premium");

            Transaction savedTransaction = new Transaction();
            savedTransaction.setId(random.nextLong(1, 100000));
            savedTransaction.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            savedTransaction.setRechargeId(rechargeId);
            savedTransaction.setUserId(userId);
            savedTransaction.setAmount(amount);
            savedTransaction.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            savedTransaction.setStatus(PaymentStatus.SUCCESS);
            savedTransaction.setRazorpayOrderId(razorpayOrderId);

            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

            PaymentResponse mockResponse = PaymentResponse.builder()
                    .status("SUCCESS")
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

            // Execute
            paymentService.processPayment(request);

            // Verify: Should publish PAYMENT_SUCCESS log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(1)).publish(logEventCaptor.capture());

            LogEvent successEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "PAYMENT_SUCCESS".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(successEvent).isNotNull();
            assertThat(successEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(successEvent.getLevel()).isEqualTo("INFO");
            assertThat(successEvent.getEventType()).isEqualTo("PAYMENT_SUCCESS");
            assertThat(successEvent.getContext()).containsKeys("transactionId", "rechargeId", "userId", "amount", "razorpayOrderId", "paymentMethod");
            assertThat(successEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(successEvent.getContext().get("userId")).isEqualTo(userId.toString());
            assertThat(successEvent.getContext().get("amount")).isEqualTo(amount.toString());
            assertThat(successEvent.getContext().get("razorpayOrderId")).isEqualTo(razorpayOrderId);
            assertThat(successEvent.getContext().get("paymentMethod")).isEqualTo("CREDIT_CARD");

            // Reset mocks for next iteration
            reset(transactionRepository, razorpayPaymentService, paymentEventProducer, logEventPublisher);
        }
    }

    /**
     * Property: For any pending payment, the system logs PAYMENT_PENDING event
     * with transactionId, rechargeId, userId, and razorpayOrderId.
     */
    @Test
    void property_paymentPending_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random payment data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));
            String razorpayOrderId = "order_" + UUID.randomUUID().toString().substring(0, 16);

            PaymentRequest request = new PaymentRequest();
            request.setRechargeId(rechargeId);
            request.setUserId(userId);
            request.setAmount(amount);
            request.setPaymentMethod("NET_BANKING");
            request.setUserEmail("user" + userId + "@example.com");
            request.setUserMobile("9876543210");
            request.setMobileNumber("9876543210");
            request.setOperatorName("Vi");
            request.setPlanName("Basic");

            Transaction savedTransaction = new Transaction();
            savedTransaction.setId(random.nextLong(1, 100000));
            savedTransaction.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            savedTransaction.setRechargeId(rechargeId);
            savedTransaction.setUserId(userId);
            savedTransaction.setAmount(amount);
            savedTransaction.setPaymentMethod(PaymentMethod.NET_BANKING);
            savedTransaction.setStatus(PaymentStatus.PENDING);
            savedTransaction.setRazorpayOrderId(razorpayOrderId);

            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

            PaymentResponse mockResponse = PaymentResponse.builder()
                    .status("PENDING")
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

            // Execute
            paymentService.processPayment(request);

            // Verify: Should publish PAYMENT_PENDING log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(1)).publish(logEventCaptor.capture());

            LogEvent pendingEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "PAYMENT_PENDING".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(pendingEvent).isNotNull();
            assertThat(pendingEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(pendingEvent.getLevel()).isEqualTo("INFO");
            assertThat(pendingEvent.getEventType()).isEqualTo("PAYMENT_PENDING");
            assertThat(pendingEvent.getContext()).containsKeys("transactionId", "rechargeId", "userId", "razorpayOrderId");
            assertThat(pendingEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(pendingEvent.getContext().get("userId")).isEqualTo(userId.toString());
            assertThat(pendingEvent.getContext().get("razorpayOrderId")).isEqualTo(razorpayOrderId);

            // Reset mocks for next iteration
            reset(transactionRepository, razorpayPaymentService, paymentEventProducer, logEventPublisher);
        }
    }

    /**
     * Property: For any failed payment, the system logs PAYMENT_FAILED event
     * with transactionId, rechargeId, userId, amount, failureReason, and paymentMethod.
     */
    @Test
    void property_paymentFailed_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random payment data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));

            PaymentRequest request = new PaymentRequest();
            request.setRechargeId(rechargeId);
            request.setUserId(userId);
            request.setAmount(amount);
            request.setPaymentMethod("DEBIT_CARD");
            request.setUserEmail("user" + userId + "@example.com");
            request.setUserMobile("9876543210");
            request.setMobileNumber("9876543210");
            request.setOperatorName("BSNL");
            request.setPlanName("Standard");

            Transaction savedTransaction = new Transaction();
            savedTransaction.setId(random.nextLong(1, 100000));
            savedTransaction.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            savedTransaction.setRechargeId(rechargeId);
            savedTransaction.setUserId(userId);
            savedTransaction.setAmount(amount);
            savedTransaction.setPaymentMethod(PaymentMethod.DEBIT_CARD);
            savedTransaction.setStatus(PaymentStatus.FAILED);
            savedTransaction.setFailureReason("Razorpay payment failed");

            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

            PaymentResponse mockResponse = PaymentResponse.builder()
                    .status("FAILED")
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

            // Execute
            paymentService.processPayment(request);

            // Verify: Should publish PAYMENT_FAILED log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(1)).publish(logEventCaptor.capture());

            LogEvent failedEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "PAYMENT_FAILED".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(failedEvent).isNotNull();
            assertThat(failedEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(failedEvent.getLevel()).isEqualTo("WARN");
            assertThat(failedEvent.getEventType()).isEqualTo("PAYMENT_FAILED");
            assertThat(failedEvent.getContext()).containsKeys("transactionId", "rechargeId", "userId", "amount", "failureReason", "paymentMethod");
            assertThat(failedEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(failedEvent.getContext().get("userId")).isEqualTo(userId.toString());
            assertThat(failedEvent.getContext().get("amount")).isEqualTo(amount.toString());
            assertThat(failedEvent.getContext().get("failureReason")).isEqualTo("Razorpay payment failed");
            assertThat(failedEvent.getContext().get("paymentMethod")).isEqualTo("DEBIT_CARD");

            // Reset mocks for next iteration
            reset(transactionRepository, razorpayPaymentService, paymentEventProducer, logEventPublisher);
        }
    }

    /**
     * Property: For any payment confirmation, the system logs PAYMENT_CONFIRMED event
     * with transactionId, rechargeId, userId, previousStatus, currentStatus, razorpayPaymentId, and amount.
     */
    @Test
    void property_paymentConfirmed_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random payment data
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));
            String razorpayPaymentId = "pay_" + UUID.randomUUID().toString().substring(0, 16);

            Transaction pendingTransaction = new Transaction();
            pendingTransaction.setId(random.nextLong(1, 100000));
            pendingTransaction.setTransactionId(transactionId);
            pendingTransaction.setRechargeId(rechargeId);
            pendingTransaction.setUserId(userId);
            pendingTransaction.setAmount(amount);
            pendingTransaction.setPaymentMethod(PaymentMethod.UPI);
            pendingTransaction.setStatus(PaymentStatus.PENDING);
            pendingTransaction.setUserEmail("user" + userId + "@example.com");
            pendingTransaction.setUserMobile("9876543210");
            pendingTransaction.setMobileNumber("9876543210");
            pendingTransaction.setOperatorName("Jio");
            pendingTransaction.setPlanName("Ultimate");

            when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

            // Execute
            paymentService.confirmPayment(transactionId, razorpayPaymentId, "sig_123");

            // Verify: Should publish PAYMENT_CONFIRMED log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());

            LogEvent confirmedEvent = logEventCaptor.getValue();
            assertThat(confirmedEvent).isNotNull();
            assertThat(confirmedEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(confirmedEvent.getLevel()).isEqualTo("INFO");
            assertThat(confirmedEvent.getEventType()).isEqualTo("PAYMENT_CONFIRMED");
            assertThat(confirmedEvent.getContext()).containsKeys("transactionId", "rechargeId", "userId", "previousStatus", "currentStatus", "razorpayPaymentId", "amount");
            assertThat(confirmedEvent.getContext().get("transactionId")).isEqualTo(transactionId);
            assertThat(confirmedEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(confirmedEvent.getContext().get("userId")).isEqualTo(userId.toString());
            assertThat(confirmedEvent.getContext().get("previousStatus")).isEqualTo("PENDING");
            assertThat(confirmedEvent.getContext().get("currentStatus")).isEqualTo("SUCCESS");
            assertThat(confirmedEvent.getContext().get("razorpayPaymentId")).isEqualTo(razorpayPaymentId);
            assertThat(confirmedEvent.getContext().get("amount")).isEqualTo(amount.toString());

            // Reset mocks for next iteration
            reset(transactionRepository, paymentEventProducer, logEventPublisher);
        }
    }
}
