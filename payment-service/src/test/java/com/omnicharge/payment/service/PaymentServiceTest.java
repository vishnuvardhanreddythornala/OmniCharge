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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

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

        // Mock rest template response since mobileNumber etc is null
        Map<String, Object> dataMap = Map.of("mobileNumber", "9876543210", "operatorName", "Jio", "planName", "Pro");
        Map<String, Object> bodyMap = Map.of("success", true, "data", dataMap);
        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(bodyMap);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        TransactionResponse response = paymentService.confirmPayment("TXN-123", "pay_webhook_id", "sign_abc");

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("9876543210", response.getMobileNumber());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class));
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
}
