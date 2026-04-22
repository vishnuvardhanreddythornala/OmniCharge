package com.omnicharge.recharge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.recharge.dto.RechargeRequest;
import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.service.IRechargeService;
import com.omnicharge.recharge.common.logging.LogEventPublisher;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RechargeController.class, excludeAutoConfiguration = {
        JpaRepositoriesAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class RechargeControllerTest {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private IRechargeService rechargeService;

    @MockitoBean
    private LogEventPublisher logEventPublisher;


    @Autowired
    public RechargeControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }
    @Test
    void initiateRecharge_Success() throws Exception {
        RechargeRequest request = new RechargeRequest();
        request.setOperatorId(1L);
        request.setPlanId(1L);
        request.setMobileNumber("9000000000");
        request.setPaymentMethod("UPI");

        RechargeResponse response = new RechargeResponse();
        response.setRechargeId(UUID.randomUUID().toString());
        response.setStatus(RechargeStatus.INITIATED);

        when(rechargeService.initiateRecharge(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/recharges")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void initiateRecharge_InvalidPayload() throws Exception {
        RechargeRequest request = new RechargeRequest(); // missing required fields

        mockMvc.perform(post("/api/recharges")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRechargeById_Success() throws Exception {
        RechargeResponse response = new RechargeResponse();
        response.setRechargeId("REC-123");

        when(rechargeService.getRechargeById(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/recharges/REC-123")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getRechargeHistory_Success() throws Exception {
        RechargeResponse response = new RechargeResponse();
        Page<RechargeResponse> page = new PageImpl<>(List.of(response));

        when(rechargeService.getRechargeHistory(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/recharges/history")
                .header("X-User-Id", "1")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getRechargeStatus_Success() throws Exception {
        when(rechargeService.getRechargeStatus("REC-123")).thenReturn("SUCCESS");

        mockMvc.perform(get("/api/recharges/status/REC-123"))
                .andExpect(status().isOk());
    }
}
