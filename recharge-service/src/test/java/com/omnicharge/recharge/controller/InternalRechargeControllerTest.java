package com.omnicharge.recharge.controller;

import com.omnicharge.recharge.common.dto.ApiResponse;
import com.omnicharge.recharge.dto.ExpiringRechargeResponse;
import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.repository.RechargeRepository;
import com.omnicharge.recharge.service.IRechargeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class InternalRechargeControllerTest {

    @Mock
    private IRechargeService rechargeService;

    @Mock
    private RechargeRepository rechargeRepository;

    @InjectMocks
    private InternalRechargeController internalRechargeController;

    private Recharge recharge;
    private ExpiringRechargeResponse expiringResponse;

    @BeforeEach
    void setUp() {
        recharge = new Recharge();
        recharge.setRechargeId("REC123");
        recharge.setUserId(1L);
        recharge.setMobileNumber("9876543210");
        recharge.setOperatorId(2L);
        recharge.setOperatorName("Jio");
        recharge.setPlanId(3L);
        recharge.setPlanName("Data Pack");
        recharge.setAmount(new BigDecimal("100"));
        recharge.setStatus(RechargeStatus.SUCCESS);

        expiringResponse = ExpiringRechargeResponse.builder().rechargeId("REC123").build();
    }

    @Test
    void getRechargeByIdInternal_Success() {
        when(rechargeRepository.findByRechargeId("REC123")).thenReturn(Optional.of(recharge));

        ResponseEntity<ApiResponse<RechargeResponse>> response = internalRechargeController.getRechargeByIdInternal("REC123");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());
        assertEquals("REC123", response.getBody().getData().getRechargeId());
        assertEquals("Jio", response.getBody().getData().getOperatorName());
    }

    @Test
    void getRechargeByIdInternal_NotFound() {
        when(rechargeRepository.findByRechargeId("REC404")).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<RechargeResponse>> response = internalRechargeController.getRechargeByIdInternal("REC404");

        assertEquals(200, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Recharge not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void getExpiringRecharges_Success() {
        when(rechargeService.getExpiringRecharges(5)).thenReturn(List.of(expiringResponse));

        ResponseEntity<ApiResponse<List<ExpiringRechargeResponse>>> response = internalRechargeController.getExpiringRecharges(5);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void getExpiredToday_Success() {
        when(rechargeService.getExpiredToday()).thenReturn(List.of(expiringResponse));

        ResponseEntity<ApiResponse<List<ExpiringRechargeResponse>>> response = internalRechargeController.getExpiredToday();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void markAsExpired_Success() {
        doNothing().when(rechargeService).markAsExpired("REC123");

        ResponseEntity<ApiResponse<Void>> response = internalRechargeController.markAsExpired("REC123");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        verify(rechargeService, times(1)).markAsExpired("REC123");
    }
}
