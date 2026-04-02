package com.omnicharge.recharge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.recharge.dto.RechargeRequest;
import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.service.IRechargeService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RechargeController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(com.omnicharge.common.logging.LogEventPublisher.class)
class RechargeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IRechargeService rechargeService;

    private RechargeRequest rechargeRequest;
    private RechargeResponse rechargeResponse;

    @BeforeEach
    void setUp() {
        rechargeRequest = new RechargeRequest();
        rechargeRequest.setMobileNumber("9876543210");
        rechargeRequest.setOperatorId(1L);
        rechargeRequest.setPlanId(10L);
        rechargeRequest.setPaymentMethod("UPI");

        rechargeResponse = RechargeResponse.builder()
                .rechargeId("OMNI-1234")
                .userId(1L)
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited 5G")
                .amount(new BigDecimal("299.00"))
                .planValidityDays(28)
                .planExpiryDate(LocalDate.now().plusDays(28))
                .status(RechargeStatus.INITIATED)
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void initiateRecharge() throws Exception {
        when(rechargeService.initiateRecharge(anyLong(), any(RechargeRequest.class)))
                .thenReturn(rechargeResponse);

        mockMvc.perform(post("/api/recharges")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rechargeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rechargeId").value("OMNI-1234"));
    }

    @Test
    void getRechargeById() throws Exception {
        when(rechargeService.getRechargeById("OMNI-1234", 1L)).thenReturn(rechargeResponse);

        mockMvc.perform(get("/api/recharges/OMNI-1234")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INITIATED"));
    }

    @Test
    void getRechargeHistory() throws Exception {
        Page<RechargeResponse> page = new PageImpl<>(Collections.singletonList(rechargeResponse));
        
        when(rechargeService.getRechargeHistory(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/recharges/history")
                        .header("X-User-Id", "1")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].operatorName").value("Airtel"));
    }

    @Test
    void getRechargeStatus() throws Exception {
        when(rechargeService.getRechargeStatus("OMNI-1234"))
                .thenReturn(RechargeStatus.SUCCESS.name()); // Overriding status dynamically for endpoint logic check

        mockMvc.perform(get("/api/recharges/status/OMNI-1234")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("SUCCESS"));
    }
}
