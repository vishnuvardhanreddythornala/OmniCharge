package com.omnicharge.recharge.controller;

import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.dto.RechargeStatsResponse;
import com.omnicharge.recharge.service.IRechargeService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminRechargeController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(com.omnicharge.common.logging.LogEventPublisher.class)
class AdminRechargeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IRechargeService rechargeService;

    @Test
    void getAllRecharges() throws Exception {
        RechargeResponse response = RechargeResponse.builder()
                .rechargeId("OMNI-ADMINVIEW")
                .operatorName("Vodafone")
                .build();
        Page<RechargeResponse> page = new PageImpl<>(Collections.singletonList(response));
        
        when(rechargeService.getAllRecharges(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/recharges")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].operatorName").value("Vodafone"));
    }

    @Test
    void getRechargeStats() throws Exception {
        RechargeStatsResponse stats = RechargeStatsResponse.builder()
                .totalRecharges(100L)
                .successCount(80L)
                .failedCount(20L)
                .totalAmount(new BigDecimal("15000.00"))
                .build();

        when(rechargeService.getRechargeStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/recharges/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(15000.00))
                .andExpect(jsonPath("$.data.successCount").value(80));
    }
}
