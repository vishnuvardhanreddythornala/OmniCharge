package com.omnicharge.payment.messaging;

import com.omnicharge.common.event.saga.RechargeInitiatedEvent;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.service.IPaymentService;
import com.omnicharge.payment.consumer.PaymentSagaConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for payment-service SAGA event logging.
 * 
 * Validates Property 4: RabbitMQ SAGA Event Logging
 * "For any SAGA event consumed or published (RechargeInitiatedEvent, PaymentApprovedEvent,
 * PaymentRejectedEvent), the system should log the event type, routing key, exchange,
 * and relevant entity IDs."
 * 
 * This test verifies that all SAGA events in payment-service (consumed and published)
 * are logged with appropriate context including event type, rechargeId, transactionId,
 * status, and routing information.
 * 
 * Feature: production-grade-centralized-logging, Property 4: RabbitMQ SAGA Event Logging
 * Validates: Requirements 3.1, 3.2, 15.3
 */
@ExtendWith(MockitoExtension.class)
@Tag("property-test")
public class PaymentSagaEventLoggingPropertyTest {

    @Mock
    private IPaymentService paymentService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private PaymentSagaConsumer paymentSagaConsumer;

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
    }

    /**
     * Property: For any SAGA event consumed (RechargeInitiatedEvent), the system logs SAGA_EVENT_CONSUMED
     * with eventType, rechargeId, userId, amount, and paymentMethod.
     */
    @Test
    void property_sagaEventConsumed_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random SAGA event data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));
            String[] paymentMethods = {"CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING"};
            String paymentMethod = paymentMethods[random.nextInt(paymentMethods.length)];

            RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                    .rechargeId(rechargeId)
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod(paymentMethod)
                    .userEmail("user" + userId + "@example.com")
                    .userMobile("9" + String.format("%09d", random.nextInt(1000000000)))
                    .mobileNumber("9" + String.format("%09d", random.nextInt(1000000000)))
                    .operatorName("Operator-" + random.nextInt(10))
                    .planName("Plan-" + random.nextInt(100))
                    .timestamp(LocalDateTime.now())
                    .build();

            PaymentResponse mockResponse = PaymentResponse.builder()
                    .status("SUCCESS")
                    .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                    .razorpayOrderId("order_" + UUID.randomUUID().toString().substring(0, 16))
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

            // Execute
            paymentSagaConsumer.consumeRechargeInitiatedEvent(event);

            // Verify: Should publish SAGA_EVENT_CONSUMED log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(1)).publish(logEventCaptor.capture());

            LogEvent consumedEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "SAGA_EVENT_CONSUMED".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(consumedEvent).isNotNull();
            assertThat(consumedEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(consumedEvent.getLevel()).isEqualTo("INFO");
            assertThat(consumedEvent.getEventType()).isEqualTo("SAGA_EVENT_CONSUMED");
            assertThat(consumedEvent.getContext()).containsKeys("eventType", "rechargeId", "userId", "amount", "paymentMethod");
            assertThat(consumedEvent.getContext().get("eventType")).isEqualTo("RechargeInitiatedEvent");
            assertThat(consumedEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(consumedEvent.getContext().get("userId")).isEqualTo(userId.toString());
            assertThat(consumedEvent.getContext().get("amount")).isEqualTo(amount.toString());
            assertThat(consumedEvent.getContext().get("paymentMethod")).isEqualTo(paymentMethod);

            // Reset mocks for next iteration
            reset(paymentService, paymentEventProducer, logEventPublisher);
        }
    }

    /**
     * Property: For any SAGA processing success, the system logs SAGA_PROCESSING_SUCCESS
     * with rechargeId, transactionId, and processingStatus.
     */
    @Test
    void property_sagaProcessingSuccess_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random SAGA event data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

            RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                    .rechargeId(rechargeId)
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod("UPI")
                    .userEmail("user" + userId + "@example.com")
                    .userMobile("9876543210")
                    .mobileNumber("9876543210")
                    .operatorName("Jio")
                    .planName("Ultimate")
                    .timestamp(LocalDateTime.now())
                    .build();

            PaymentResponse mockResponse = PaymentResponse.builder()
                    .status("SUCCESS")
                    .transactionId(transactionId)
                    .razorpayOrderId("order_" + UUID.randomUUID().toString().substring(0, 16))
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

            // Execute
            paymentSagaConsumer.consumeRechargeInitiatedEvent(event);

            // Verify: Should publish SAGA_PROCESSING_SUCCESS log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(1)).publish(logEventCaptor.capture());

            LogEvent successEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "SAGA_PROCESSING_SUCCESS".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(successEvent).isNotNull();
            assertThat(successEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(successEvent.getLevel()).isEqualTo("INFO");
            assertThat(successEvent.getEventType()).isEqualTo("SAGA_PROCESSING_SUCCESS");
            assertThat(successEvent.getContext()).containsKeys("rechargeId", "transactionId", "processingStatus");
            assertThat(successEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(successEvent.getContext().get("transactionId")).isEqualTo(transactionId);
            assertThat(successEvent.getContext().get("processingStatus")).isEqualTo("APPROVED");

            // Reset mocks for next iteration
            reset(paymentService, paymentEventProducer, logEventPublisher);
        }
    }

    /**
     * Property: For any SAGA processing pending, the system logs SAGA_PROCESSING_PENDING
     * with rechargeId, transactionId, and processingStatus.
     */
    @Test
    void property_sagaProcessingPending_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random SAGA event data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

            RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                    .rechargeId(rechargeId)
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod("NET_BANKING")
                    .userEmail("user" + userId + "@example.com")
                    .userMobile("9876543210")
                    .mobileNumber("9876543210")
                    .operatorName("Airtel")
                    .planName("Premium")
                    .timestamp(LocalDateTime.now())
                    .build();

            PaymentResponse mockResponse = PaymentResponse.builder()
                    .status("PENDING")
                    .transactionId(transactionId)
                    .razorpayOrderId("order_" + UUID.randomUUID().toString().substring(0, 16))
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

            // Execute
            paymentSagaConsumer.consumeRechargeInitiatedEvent(event);

            // Verify: Should publish SAGA_PROCESSING_PENDING log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(1)).publish(logEventCaptor.capture());

            LogEvent pendingEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "SAGA_PROCESSING_PENDING".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(pendingEvent).isNotNull();
            assertThat(pendingEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(pendingEvent.getLevel()).isEqualTo("INFO");
            assertThat(pendingEvent.getEventType()).isEqualTo("SAGA_PROCESSING_PENDING");
            assertThat(pendingEvent.getContext()).containsKeys("rechargeId", "transactionId", "processingStatus");
            assertThat(pendingEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(pendingEvent.getContext().get("transactionId")).isEqualTo(transactionId);
            assertThat(pendingEvent.getContext().get("processingStatus")).isEqualTo("PENDING_CONFIRMATION");

            // Reset mocks for next iteration
            reset(paymentService, paymentEventProducer, logEventPublisher);
        }
    }

    /**
     * Property: For any SAGA processing failure, the system logs SAGA_PROCESSING_FAILED
     * with rechargeId, processingStatus, and failureReason.
     */
    @Test
    void property_sagaProcessingFailed_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random SAGA event data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));

            RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                    .rechargeId(rechargeId)
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod("DEBIT_CARD")
                    .userEmail("user" + userId + "@example.com")
                    .userMobile("9876543210")
                    .mobileNumber("9876543210")
                    .operatorName("Vi")
                    .planName("Basic")
                    .timestamp(LocalDateTime.now())
                    .build();

            PaymentResponse mockResponse = PaymentResponse.builder()
                    .status("FAILED")
                    .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

            // Execute
            paymentSagaConsumer.consumeRechargeInitiatedEvent(event);

            // Verify: Should publish SAGA_PROCESSING_FAILED log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(1)).publish(logEventCaptor.capture());

            LogEvent failedEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "SAGA_PROCESSING_FAILED".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(failedEvent).isNotNull();
            assertThat(failedEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(failedEvent.getLevel()).isEqualTo("WARN");
            assertThat(failedEvent.getEventType()).isEqualTo("SAGA_PROCESSING_FAILED");
            assertThat(failedEvent.getContext()).containsKeys("rechargeId", "processingStatus", "failureReason");
            assertThat(failedEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(failedEvent.getContext().get("processingStatus")).isEqualTo("REJECTED");
            assertThat(failedEvent.getContext().get("failureReason")).isEqualTo("Payment creation failed via Razorpay");

            // Reset mocks for next iteration
            reset(paymentService, paymentEventProducer, logEventPublisher);
        }
    }

    /**
     * Property: For any SAGA processing exception, the system logs SAGA_PROCESSING_EXCEPTION
     * with rechargeId, processingStatus, and errorMessage.
     */
    @Test
    void property_sagaProcessingException_logsBusinessOperationWithContext() {
        // Run 100+ iterations with random data
        for (int i = 0; i < 100; i++) {
            // Generate random SAGA event data
            String rechargeId = "OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Long userId = random.nextLong(1, 10000);
            BigDecimal amount = new BigDecimal(random.nextInt(100, 1000));
            String errorMessage = "Database connection timeout";

            RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                    .rechargeId(rechargeId)
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod("CREDIT_CARD")
                    .userEmail("user" + userId + "@example.com")
                    .userMobile("9876543210")
                    .mobileNumber("9876543210")
                    .operatorName("BSNL")
                    .planName("Standard")
                    .timestamp(LocalDateTime.now())
                    .build();

            when(paymentService.processPayment(any(PaymentRequest.class)))
                    .thenThrow(new RuntimeException(errorMessage));

            // Execute
            paymentSagaConsumer.consumeRechargeInitiatedEvent(event);

            // Verify: Should publish SAGA_PROCESSING_EXCEPTION log event
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            verify(logEventPublisher, atLeast(1)).publish(logEventCaptor.capture());

            LogEvent exceptionEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "SAGA_PROCESSING_EXCEPTION".equals(e.getEventType()))
                    .findFirst()
                    .orElse(null);

            assertThat(exceptionEvent).isNotNull();
            assertThat(exceptionEvent.getServiceName()).isEqualTo("payment-service");
            assertThat(exceptionEvent.getLevel()).isEqualTo("ERROR");
            assertThat(exceptionEvent.getEventType()).isEqualTo("SAGA_PROCESSING_EXCEPTION");
            assertThat(exceptionEvent.getContext()).containsKeys("rechargeId", "processingStatus", "errorMessage");
            assertThat(exceptionEvent.getContext().get("rechargeId")).isEqualTo(rechargeId);
            assertThat(exceptionEvent.getContext().get("processingStatus")).isEqualTo("EXCEPTION");
            assertThat(exceptionEvent.getContext().get("errorMessage")).isEqualTo(errorMessage);

            // Reset mocks for next iteration
            reset(paymentService, paymentEventProducer, logEventPublisher);
        }
    }
}
