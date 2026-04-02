package com.omnicharge.payment.controller;

import com.omnicharge.payment.dto.PaymentStatsResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.service.IPaymentService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(com.omnicharge.common.logging.LogEventPublisher.class)
class AdminPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IPaymentService paymentService;

    @Test
    void getAllTransactions_SuccessWhenAdmin() throws Exception {
        TransactionResponse response = TransactionResponse.builder().transactionId("TXN-ADMIN").build();
        Page<TransactionResponse> page = new PageImpl<>(Collections.singletonList(response));
        
        when(paymentService.getAllTransactions(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/payments")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].transactionId").value("TXN-ADMIN"));
    }

    @Test
    void getAllTransactions_ForbiddenWhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/payments")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied: Admin role required"));
    }

    @Test
    void getPaymentStats_Success() throws Exception {
        PaymentStatsResponse stats = PaymentStatsResponse.builder().totalTransactions(500L).topUsers(Collections.emptyList()).build();
        when(paymentService.getPaymentStats(anyInt())).thenReturn(stats);

        mockMvc.perform(get("/api/admin/payments/stats")
                        .header("X-User-Role", "ADMIN")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTransactions").value(500));
    }

    @Test
    void getPaymentStats_ForbiddenWhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/payments/stats")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
