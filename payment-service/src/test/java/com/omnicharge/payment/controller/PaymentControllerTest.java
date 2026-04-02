package com.omnicharge.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.service.IPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(com.omnicharge.common.logging.LogEventPublisher.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IPaymentService paymentService;

    private PaymentRequest paymentRequest;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest();
        paymentRequest.setRechargeId("OMNI-1234");
        paymentRequest.setUserId(1L);
        paymentRequest.setAmount(new BigDecimal("299.00"));
        paymentRequest.setPaymentMethod("CREDIT_CARD");

        paymentResponse = PaymentResponse.builder()
                .transactionId("TXN-123")
                .status("SUCCESS")
                .amount(new BigDecimal("299.00"))
                .build();
    }

    @Test
    void processPayment_Success() throws Exception {
        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/payments/process")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value("TXN-123"));
    }

    @Test
    void processPayment_UnauthorizedUserMismatch_ReturnsBadRequest() throws Exception {
        // Here X-User-Id is 2, but paymentRequest.userId is 1
        mockMvc.perform(post("/api/payments/process")
                        .header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized: Cannot create payment for another user"));
    }

    @Test
    void getTransaction_Found() throws Exception {
        TransactionResponse tx = TransactionResponse.builder().transactionId("TXN-123").build();
        when(paymentService.getTransaction("TXN-123", 1L)).thenReturn(tx);

        mockMvc.perform(get("/api/payments/TXN-123")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getPaymentHistory() throws Exception {
        TransactionResponse tx = TransactionResponse.builder().transactionId("TXN-123").build();
        Page<TransactionResponse> page = new PageImpl<>(Collections.singletonList(tx));
        when(paymentService.getPaymentHistory(eq(1L), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/payments/history")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].transactionId").value("TXN-123"));
    }

    @Test
    void confirmPaymentManually() throws Exception {
        TransactionResponse tx = TransactionResponse.builder().transactionId("TXN-CONFIRMED").status(com.omnicharge.payment.entity.PaymentStatus.SUCCESS).build();
        when(paymentService.confirmPayment("TXN-CONFIRMED", "mockRzId", "mockSig")).thenReturn(tx);

        mockMvc.perform(post("/api/payments/webhook/confirm/TXN-CONFIRMED")
                        .param("razorpayPaymentId", "mockRzId")
                        .param("razorpaySignature", "mockSig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value("TXN-CONFIRMED"));
    }
}
