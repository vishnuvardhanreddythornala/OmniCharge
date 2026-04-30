package com.omnicharge.payment.service;

import com.omnicharge.payment.common.exception.BadRequestException;
import com.omnicharge.payment.common.exception.ResourceNotFoundException;
import com.omnicharge.payment.common.logging.LogEvent;
import com.omnicharge.payment.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.dto.PaymentStatsResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.entity.PaymentMethod;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.springframework.http.ResponseEntity;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IRazorpayPaymentService razorpayPaymentService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private com.omnicharge.payment.client.RechargeServiceClient rechargeServiceClient;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest validRequest;
    private Transaction pendingTransaction;

    @BeforeEach
    void setUp() {
        validRequest = new PaymentRequest();
        validRequest.setRechargeId("OMNI-RECHARGE-123");
        validRequest.setUserId(1L);
        validRequest.setAmount(new BigDecimal("199.00"));
        validRequest.setPaymentMethod("UPI");
        validRequest.setUserEmail("test@example.com");
        validRequest.setUserMobile("9876543210");
        validRequest.setMobileNumber("9876543210");
        validRequest.setOperatorName("Airtel");
        validRequest.setPlanName("Basic Plan");

        pendingTransaction = new Transaction();
        pendingTransaction.setId(100L);
        pendingTransaction.setTransactionId("TXN-ASDFGHJKL");
        pendingTransaction.setRechargeId("OMNI-RECHARGE-123");
        pendingTransaction.setUserId(1L);
        pendingTransaction.setAmount(new BigDecimal("199.00"));
        pendingTransaction.setPaymentMethod(PaymentMethod.UPI);
        pendingTransaction.setStatus(PaymentStatus.PENDING);
        pendingTransaction.setUserEmail("test@example.com");
    }

    @Test
    void processPayment_SuccessFlow() {
        PaymentResponse mockRazorpayResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .razorpayOrderId("order_abc123")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockRazorpayResponse);

        PaymentResponse response = paymentService.processPayment(validRequest);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("order_abc123", response.getRazorpayOrderId());
        verify(logEventPublisher, atLeastOnce()).publish(any(LogEvent.class));
        verify(paymentEventProducer, times(1)).publishPaymentCompleted(any());
    }

    @Test
    void processPayment_PendingFlow() {
        PaymentResponse mockRazorpayResponse = PaymentResponse.builder()
                .status("PENDING")
                .razorpayOrderId("order_abc123")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockRazorpayResponse);

        PaymentResponse response = paymentService.processPayment(validRequest);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals("order_abc123", response.getRazorpayOrderId());
        verify(paymentEventProducer, never()).publishPaymentCompleted(any());
    }

    @Test
    void processPayment_FailedFlow() {
        PaymentResponse mockRazorpayResponse = PaymentResponse.builder()
                .status("FAILED")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockRazorpayResponse);

        PaymentResponse response = paymentService.processPayment(validRequest);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        verify(paymentEventProducer, times(1)).publishPaymentCompleted(any()); // Triggered on failure terminal states
    }

    @Test
    void confirmPayment_SuccessWithMissingMetadataFetch() {
        Transaction missingDataTxn = new Transaction();
        missingDataTxn.setTransactionId("TXN-123");
        missingDataTxn.setRechargeId("OMNI-123");
        missingDataTxn.setUserId(1L);
        missingDataTxn.setStatus(PaymentStatus.PENDING);
        missingDataTxn.setAmount(BigDecimal.TEN);

        when(transactionRepository.findByTransactionId("TXN-123")).thenReturn(Optional.of(missingDataTxn));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Mock Feign client response since mobileNumber etc is null
        Map<String, Object> dataMap = Map.of("mobileNumber", "9876543210", "operatorName", "Jio", "planName", "Pro");
        Map<String, Object> bodyMap = Map.of("success", true, "data", dataMap);
        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(bodyMap);

        when(rechargeServiceClient.getRechargeById("OMNI-123"))
                .thenReturn(responseEntity);

        TransactionResponse response = paymentService.confirmPayment("TXN-123", "pay_webhook_id", "sign_abc");

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("9876543210", response.getMobileNumber());
        verify(rechargeServiceClient, times(1)).getRechargeById("OMNI-123");
        verify(paymentEventProducer, times(1)).publishPaymentApproved(any());
    }

    @Test
    void confirmPayment_IdempotencyCheck() {
        Transaction successTxn = new Transaction();
        successTxn.setTransactionId("TXN-123");
        successTxn.setStatus(PaymentStatus.SUCCESS);

        when(transactionRepository.findByTransactionId("TXN-123")).thenReturn(Optional.of(successTxn));

        TransactionResponse response = paymentService.confirmPayment("TXN-123", "pay_webhook_id", "sign_abc");

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        verify(transactionRepository, never()).save(any());
        verify(paymentEventProducer, never()).publishPaymentApproved(any());
    }

    @Test
    void failPayment_Success() {
        when(transactionRepository.findByTransactionId(pendingTransaction.getTransactionId()))
                .thenReturn(Optional.of(pendingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = paymentService.failPayment(pendingTransaction.getTransactionId(), "User Cancelled");

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        verify(paymentEventProducer, times(1)).publishPaymentRejected(any());
    }

    @Test
    void failPayment_AlreadyFailedIdempotency() {
        pendingTransaction.setStatus(PaymentStatus.FAILED);
        when(transactionRepository.findByTransactionId(pendingTransaction.getTransactionId()))
                .thenReturn(Optional.of(pendingTransaction));

        TransactionResponse response = paymentService.failPayment(pendingTransaction.getTransactionId(), "Duplicate Cancel");

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        verify(transactionRepository, never()).save(any());
        verify(paymentEventProducer, never()).publishPaymentRejected(any());
    }

    @Test
    void failPayment_NotAllowedIfSuccess() {
        pendingTransaction.setStatus(PaymentStatus.SUCCESS);
        when(transactionRepository.findByTransactionId(pendingTransaction.getTransactionId()))
                .thenReturn(Optional.of(pendingTransaction));

        TransactionResponse response = paymentService.failPayment(pendingTransaction.getTransactionId(), "Late Cancel Call");

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        verify(transactionRepository, never()).save(any());
        verify(paymentEventProducer, never()).publishPaymentRejected(any());
    }

    @Test
    void getTransaction_Success() {
        when(transactionRepository.findByTransactionId(pendingTransaction.getTransactionId()))
                .thenReturn(Optional.of(pendingTransaction));

        TransactionResponse response = paymentService.getTransaction(pendingTransaction.getTransactionId(), 1L);

        assertEquals(pendingTransaction.getTransactionId(), response.getTransactionId());
    }

    @Test
    void getTransaction_Unauthorized() {
        when(transactionRepository.findByTransactionId(pendingTransaction.getTransactionId()))
                .thenReturn(Optional.of(pendingTransaction));

        assertThrows(BadRequestException.class, () -> {
            paymentService.getTransaction(pendingTransaction.getTransactionId(), 999L);
        });
    }

    @Test
    void getTransaction_NotFound() {
        when(transactionRepository.findByTransactionId("MISSING")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.getTransaction("MISSING", 1L);
        });
    }
    
    @Test
    void getPaymentHistory_Success() {
        Page<Transaction> page = new PageImpl<>(List.of(pendingTransaction));
        when(transactionRepository.findByUserIdWithFilters(anyLong(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);
                
        Page<TransactionResponse> result = paymentService.getPaymentHistory(1L, null, null, null, null, null, null, PageRequest.of(0, 10));
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void getAllTransactions_Success() {
        Page<Transaction> page = new PageImpl<>(List.of(pendingTransaction));
        when(transactionRepository.findAllWithFilters(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);
                
        Page<TransactionResponse> result = paymentService.getAllTransactions(null, null, null, null, null, null, null, PageRequest.of(0, 10));
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void getPaymentStats_Success() {
        when(transactionRepository.count()).thenReturn(100L);
        when(transactionRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(80L);
        when(transactionRepository.sumAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(new BigDecimal("15000"));
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.findTopUsersByRevenue(any(), any())).thenReturn(Collections.emptyList());
        
        PaymentStatsResponse stats = paymentService.getPaymentStats(30);
        
        assertNotNull(stats);
        assertEquals(100L, stats.getTotalTransactions());
        assertEquals(new BigDecimal("15000"), stats.getTotalRevenue());
    }

    @Test
    void getPaymentStats_DefaultDays() {
        when(transactionRepository.count()).thenReturn(10L);
        when(transactionRepository.countByStatus(any())).thenReturn(5L);
        when(transactionRepository.sumAmountByStatus(any())).thenReturn(BigDecimal.TEN);
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.findTopUsersByRevenue(any(), any())).thenReturn(Collections.emptyList());

        PaymentStatsResponse stats = paymentService.getPaymentStats(null);
        assertNotNull(stats);
    }

    @Test
    void processPayment_UnknownPaymentMethod() {
        validRequest.setPaymentMethod("BITCOIN");

        PaymentResponse mockRazorpayResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .razorpayOrderId("order_abc123")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockRazorpayResponse);

        PaymentResponse response = paymentService.processPayment(validRequest);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void confirmPayment_WithoutMissingMetadata() {
        // Transaction already has metadata - no enrichment needed
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN-FULL");
        txn.setRechargeId("REC-FULL");
        txn.setUserId(1L);
        txn.setStatus(PaymentStatus.PENDING);
        txn.setAmount(BigDecimal.TEN);
        txn.setMobileNumber("9876543210");
        txn.setOperatorName("Jio");
        txn.setPlanName("Gold");
        txn.setPaymentMethod(PaymentMethod.UPI);

        when(transactionRepository.findByTransactionId("TXN-FULL")).thenReturn(Optional.of(txn));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = paymentService.confirmPayment("TXN-FULL", "pay_123", "sig_123");

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        verify(rechargeServiceClient, never()).getRechargeById(any());
    }

    @Test
    void confirmPayment_EnrichmentReturnsNoData() {
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN-NODATA");
        txn.setRechargeId("REC-NODATA");
        txn.setUserId(1L);
        txn.setStatus(PaymentStatus.PENDING);
        txn.setAmount(BigDecimal.TEN);
        txn.setPaymentMethod(PaymentMethod.UPI);

        when(transactionRepository.findByTransactionId("TXN-NODATA")).thenReturn(Optional.of(txn));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Return success=false from recharge-service
        Map<String, Object> bodyMap = Map.of("success", false);
        when(rechargeServiceClient.getRechargeById("REC-NODATA"))
                .thenReturn(ResponseEntity.ok(bodyMap));

        TransactionResponse response = paymentService.confirmPayment("TXN-NODATA", "pay_123", "sig_123");

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
    }

    @Test
    void confirmPayment_EnrichmentThrowsException() {
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN-ERR");
        txn.setRechargeId("REC-ERR");
        txn.setUserId(1L);
        txn.setStatus(PaymentStatus.PENDING);
        txn.setAmount(BigDecimal.TEN);
        txn.setPaymentMethod(PaymentMethod.UPI);

        when(transactionRepository.findByTransactionId("TXN-ERR")).thenReturn(Optional.of(txn));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(rechargeServiceClient.getRechargeById("REC-ERR")).thenThrow(new RuntimeException("Connection refused"));

        TransactionResponse response = paymentService.confirmPayment("TXN-ERR", "pay_123", "sig_123");

        // Should not throw — enrichment failure is caught
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
    }

    @Test
    void confirmPayment_TransactionNotFound() {
        when(transactionRepository.findByTransactionId("TXN-MISSING")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.confirmPayment("TXN-MISSING", "pay_123", "sig_123"));
    }

    @Test
    void failPayment_TransactionNotFound() {
        when(transactionRepository.findByTransactionId("TXN-MISSING")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.failPayment("TXN-MISSING", "cancelled"));
    }

    @Test
    void processPayment_PublishPaymentEventThrowsException() {
        PaymentResponse mockRazorpayResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .razorpayOrderId("order_abc123")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockRazorpayResponse);
        org.mockito.Mockito.doThrow(new RuntimeException("RabbitMQ down")).when(paymentEventProducer).publishPaymentCompleted(any());

        PaymentResponse response = paymentService.processPayment(validRequest);

        // Should not throw — event publish failure is caught
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void getPaymentStats_WithRevenueData() {
        when(transactionRepository.count()).thenReturn(50L);
        when(transactionRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(40L);
        when(transactionRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(5L);
        when(transactionRepository.countByStatus(PaymentStatus.PENDING)).thenReturn(5L);
        when(transactionRepository.sumAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(new BigDecimal("10000"));
        when(transactionRepository.sumAmountByStatus(PaymentStatus.FAILED)).thenReturn(new BigDecimal("500"));
        when(transactionRepository.averageAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(new BigDecimal("250"));
        when(transactionRepository.countTransactionsSince(any())).thenReturn(10L);
        when(transactionRepository.sumAmountSinceByStatus(any(), any())).thenReturn(new BigDecimal("2500"));

        Object[] row1 = new Object[]{"2026-04-29", 10L, new BigDecimal("2500")};
        List<Object[]> revenueRows = new java.util.ArrayList<>();
        revenueRows.add(row1);
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(revenueRows);

        Object[] userRow = new Object[]{1L, 5L, new BigDecimal("1000")};
        List<Object[]> userRows = new java.util.ArrayList<>();
        userRows.add(userRow);
        when(transactionRepository.findTopUsersByRevenue(any(), any())).thenReturn(userRows);

        PaymentStatsResponse stats = paymentService.getPaymentStats(7);

        assertNotNull(stats);
        assertEquals(50L, stats.getTotalTransactions());
        assertEquals(1, stats.getRevenueByDate().size());
        assertEquals(1, stats.getTopUsers().size());
    }
}
