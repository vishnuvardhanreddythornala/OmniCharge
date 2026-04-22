package com.omnicharge.recharge.controller;
import static org.mockito.ArgumentMatchers.isNull;


import com.omnicharge.recharge.common.dto.ApiResponse;
import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.dto.RechargeStatsResponse;
import com.omnicharge.recharge.service.IRechargeService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AdminRechargeControllerTest {

    @Mock
    private IRechargeService rechargeService;

    @InjectMocks
    private AdminRechargeController adminRechargeController;

    private RechargeResponse rechargeResponse;

    @BeforeEach
    void setUp() {
        rechargeResponse = RechargeResponse.builder()
                .rechargeId("REC123")
                .userId(1L)
                .amount(new java.math.BigDecimal("199.00"))
                .build();
    }

    @Test
    void getAllRecharges_Success_Descending() {
        Page<RechargeResponse> page = new PageImpl<>(List.of(rechargeResponse));
        
        when(rechargeService.getAllRecharges(
                eq("SUCCESS"), any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(page);
                
        ResponseEntity<ApiResponse<Page<RechargeResponse>>> response = adminRechargeController.getAllRecharges(
                0, 10, "createdDate", "DESC",
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), "SUCCESS");
                
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().getTotalElements());
    }

    @Test
    void getAllRecharges_Success_Ascending() {
        Page<RechargeResponse> page = new PageImpl<>(List.of(rechargeResponse));
        
        when(rechargeService.getAllRecharges(
                isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);
                
        ResponseEntity<ApiResponse<Page<RechargeResponse>>> response = adminRechargeController.getAllRecharges(
                0, 10, "amount", "ASC",
                null, null, null);
                
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().getTotalElements());
    }

    @Test
    void getRechargeStats_Success() {
        RechargeStatsResponse stats = RechargeStatsResponse.builder()
                .totalRecharges(100L)
                .build();
                
        when(rechargeService.getRechargeStats()).thenReturn(stats);
        
        ResponseEntity<ApiResponse<RechargeStatsResponse>> response = adminRechargeController.getRechargeStats();
        
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals(100L, response.getBody().getData().getTotalRecharges());
    }
}
