package com.omnicharge.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.service.IPaymentService;
import com.omnicharge.payment.common.logging.LogEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = PaymentController.class, excludeAutoConfiguration = {
        JpaRepositoriesAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IPaymentService paymentService;

    @MockitoBean
    private LogEventPublisher logEventPublisher;

    @Test
    void processPayment_Success() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setUserId(1L);
        request.setRechargeId(UUID.randomUUID().toString());
        request.setAmount(new BigDecimal("199.00"));
        request.setPaymentMethod("UPI");

        PaymentResponse response = new PaymentResponse();
        response.setTransactionId("TXN-123");
        response.setStatus(PaymentStatus.SUCCESS.name());

        when(paymentService.processPayment(any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/process")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionId").value("TXN-123"));
    }

    @Test
    void processPayment_UnauthorizedUserMismatch() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setUserId(2L); // Different user ID from header
        request.setRechargeId(UUID.randomUUID().toString());
        request.setAmount(new BigDecimal("199.00"));
        request.setPaymentMethod("UPI");

        mockMvc.perform(post("/api/payments/process")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransaction_Success() throws Exception {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");

        when(paymentService.getTransaction("TXN-123", 1L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/TXN-123")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionId").value("TXN-123"));
    }

    @Test
    void confirmPaymentManually_Success() throws Exception {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");
        response.setStatus(PaymentStatus.SUCCESS);

        when(paymentService.confirmPayment(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/webhook/confirm/TXN-123")
                .param("razorpayPaymentId", "pay_abc")
                .param("razorpaySignature", "sign_abc"))
                .andExpect(status().isOk());
    }

    @Test
    void failPayment_Success() throws Exception {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");
        response.setStatus(PaymentStatus.FAILED);

        when(paymentService.failPayment(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/webhook/fail/TXN-123")
                .param("reason", "Cancelled"))
                .andExpect(status().isOk());
    }

    @Test
    void getPaymentHistory_Success() throws Exception {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId("TXN-123");
        Page<TransactionResponse> page = new PageImpl<>(List.of(response));

        when(paymentService.getPaymentHistory(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/payments/history")
                .header("X-User-Id", "1")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }
}
