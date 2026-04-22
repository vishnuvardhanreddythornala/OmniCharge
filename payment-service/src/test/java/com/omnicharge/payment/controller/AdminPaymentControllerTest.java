package com.omnicharge.payment.controller;
import static org.mockito.ArgumentMatchers.isNull;


import com.omnicharge.payment.common.dto.ApiResponse;
import com.omnicharge.payment.dto.PaymentStatsResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.service.IPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class AdminPaymentControllerTest {

    @Mock
    private IPaymentService paymentService;

    @InjectMocks
    private AdminPaymentController adminPaymentController;

    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        transactionResponse = TransactionResponse.builder()
                .transactionId("TXN123")
                .rechargeId("REC123")
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.SUCCESS)
                .build();
    }

    @Test
    void getAllTransactions_Success_AdminRole() {
        Page<TransactionResponse> page = new PageImpl<>(List.of(transactionResponse));

        when(paymentService.getAllTransactions(
                anyLong(), any(), any(), any(), any(), any(), anyString(), any(PageRequest.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Page<TransactionResponse>>> response = adminPaymentController.getAllTransactions(
                "ROLE_ADMIN", 1L, new BigDecimal("50"), new BigDecimal("150"),
                PaymentStatus.SUCCESS, LocalDateTime.now().minusDays(1), LocalDateTime.now(),
                "REC123", 0, 10, "createdDate", "DESC");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().getTotalElements());
    }

    @Test
    void getAllTransactions_Success_AscendingSort() {
        Page<TransactionResponse> page = new PageImpl<>(List.of(transactionResponse));

        when(paymentService.getAllTransactions(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Page<TransactionResponse>>> response = adminPaymentController.getAllTransactions(
                "ROLE_ADMIN", null, null, null, null, null, null, null,
                0, 10, "amount", "ASC");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void getAllTransactions_Forbidden_NotAdmin() {
        ResponseEntity<ApiResponse<Page<TransactionResponse>>> response = adminPaymentController.getAllTransactions(
                "ROLE_USER", null, null, null, null, null, null, null,
                0, 10, "createdDate", "DESC");

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Access denied: Admin role required", response.getBody().getMessage());

        verify(paymentService, never()).getAllTransactions(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getPaymentStats_Success_AdminRole() {
        PaymentStatsResponse stats = PaymentStatsResponse.builder().totalTransactions(100L).build();

        when(paymentService.getPaymentStats(30)).thenReturn(stats);

        ResponseEntity<ApiResponse<PaymentStatsResponse>> response = adminPaymentController.getPaymentStats(
                "ROLE_ADMIN", 30);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(100L, response.getBody().getData().getTotalTransactions());
    }

    @Test
    void getPaymentStats_Forbidden_NotAdmin() {
        ResponseEntity<ApiResponse<PaymentStatsResponse>> response = adminPaymentController.getPaymentStats(
                "ROLE_USER", 30);

        assertEquals(403, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Access denied: Admin role required", response.getBody().getMessage());

        verify(paymentService, never()).getPaymentStats(anyInt());
    }
}
