package com.omnicharge.recharge.service;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.recharge.client.OperatorServiceClient;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.*;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.messaging.RechargeEventProducer;
import com.omnicharge.recharge.repository.RechargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RechargeServiceTest {

    @Mock
    private RechargeRepository rechargeRepository;

    @Mock
    private OperatorServiceClient operatorServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private RechargeEventProducer rechargeEventProducer;

    @Mock
    private com.omnicharge.common.logging.LogEventPublisher logEventPublisher;

    @InjectMocks
    private RechargeService rechargeService;

    private RechargeRequest rechargeRequest;
    private PlanResponse planResponse;
    private UserProfileResponse userProfileResponse;
    private Recharge recharge;

    @BeforeEach
    void setUp() {
        rechargeRequest = new RechargeRequest();
        rechargeRequest.setMobileNumber("9876543210");
        rechargeRequest.setOperatorId(1L);
        rechargeRequest.setPlanId(10L);
        rechargeRequest.setPaymentMethod("UPI");

        planResponse = PlanResponse.builder()
                .id(10L)
                .operatorId(1L)
                .operatorName("Airtel")
                .planName("Unlimited 5G")
                .price(new BigDecimal("299.00"))
                .validityDays(28)
                .isActive(true)
                .build();

        userProfileResponse = UserProfileResponse.builder()
                .id(1L)
                .email("test@example.com")
                .mobileNumber("9876543210")
                .build();

        recharge = new Recharge();
        recharge.setId(100L);
        recharge.setRechargeId("OMNI-A1B2C3D4");
        recharge.setUserId(1L);
        recharge.setMobileNumber("9876543210");
        recharge.setOperatorId(1L);
        recharge.setOperatorName("Airtel");
        recharge.setPlanId(10L);
        recharge.setPlanName("Unlimited 5G");
        recharge.setAmount(new BigDecimal("299.00"));
        recharge.setPlanValidityDays(28);
        recharge.setPlanExpiryDate(LocalDate.now().plusDays(28));
        recharge.setStatus(RechargeStatus.SUCCESS);
        recharge.setCreatedDate(LocalDateTime.now());
    }

    @Test
    void initiateRecharge_Success() {
        when(operatorServiceClient.getPlan(anyLong()))
                .thenReturn(ApiResponse.success("Success", planResponse));
        when(userServiceClient.getUserById(anyLong()))
                .thenReturn(ApiResponse.success("Success", userProfileResponse));

        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(invocation -> {
            Recharge r = invocation.getArgument(0);
            r.setRechargeId("OMNI-MOCK123");
            return r;
        });

        doNothing().when(rechargeEventProducer).publishRechargeInitiated(any());

        RechargeResponse response = rechargeService.initiateRecharge(1L, rechargeRequest);

        assertNotNull(response);
        assertEquals(RechargeStatus.PROCESSING, response.getStatus());
        verify(rechargeEventProducer, times(1)).publishRechargeInitiated(any());
        verify(rechargeRepository, times(2)).save(any(Recharge.class));
    }

    @Test
    void initiateRecharge_CircuitBreaker_OperatorDown_ThrowsException() {
        // Simulates Resilience4j fallback returning null or an error map from circuit breaker intercept
        when(operatorServiceClient.getPlan(anyLong())).thenReturn(null);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
            rechargeService.initiateRecharge(1L, rechargeRequest)
        );
        assertTrue(ex.getMessage().contains("Operator Service is temporarily unavailable"));
        verify(rechargeRepository, never()).save(any());
    }

    @Test
    void initiateRecharge_OperatorAPI_ReturnsError_ThrowsException() {
        when(operatorServiceClient.getPlan(anyLong())).thenReturn(ApiResponse.error("Internal Server Error"));

        assertThrows(BadRequestException.class, () -> rechargeService.initiateRecharge(1L, rechargeRequest));
    }

    @Test
    void initiateRecharge_PlanInactive_ThrowsException() {
        planResponse.setIsActive(false);
        when(operatorServiceClient.getPlan(anyLong())).thenReturn(ApiResponse.success("Success", planResponse));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                rechargeService.initiateRecharge(1L, rechargeRequest)
        );
        assertEquals("Invalid or inactive plan", ex.getMessage());
    }

    @Test
    void initiateRecharge_PlanOperatorMismatch_ThrowsException() {
        planResponse.setOperatorId(99L); // Differs from rechargeRequest (1L)
        when(operatorServiceClient.getPlan(anyLong())).thenReturn(ApiResponse.success("Success", planResponse));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                rechargeService.initiateRecharge(1L, rechargeRequest)
        );
        assertEquals("Plan does not belong to the specified operator", ex.getMessage());
    }

    @Test
    void initiateRecharge_UserServiceDown_StillSucceedsWithoutUserDetails() {
        when(operatorServiceClient.getPlan(anyLong()))
                .thenReturn(ApiResponse.success("Success", planResponse));
        // Fallback or outage returns null or error for UserServiceClient
        when(userServiceClient.getUserById(anyLong())).thenThrow(new RuntimeException("API DOWN"));

        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(i -> i.getArgument(0));

        // It should gracefully catch the exception from UserServiceClient and proceed
        assertDoesNotThrow(() -> rechargeService.initiateRecharge(1L, rechargeRequest));
        verify(rechargeEventProducer, times(1)).publishRechargeInitiated(any());
    }

    @Test
    void getRechargeById_Success() {
        when(rechargeRepository.findByRechargeId("OMNI-A1B2C3D4")).thenReturn(Optional.of(recharge));

        RechargeResponse response = rechargeService.getRechargeById("OMNI-A1B2C3D4", 1L);

        assertNotNull(response);
        assertEquals("OMNI-A1B2C3D4", response.getRechargeId());
    }

    @Test
    void getRechargeById_Unauthorized() {
        when(rechargeRepository.findByRechargeId("OMNI-A1B2C3D4")).thenReturn(Optional.of(recharge));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                rechargeService.getRechargeById("OMNI-A1B2C3D4", 99L) // Wrong user ID
        );
        assertEquals("Unauthorized access to recharge", ex.getMessage());
    }

    @Test
    void getExpiringRecharges_Success() {
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(recharge));
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.success("OK", userProfileResponse));

        List<ExpiringRechargeResponse> responses = rechargeService.getExpiringRecharges(5);

        assertFalse(responses.isEmpty());
        assertEquals("test@example.com", responses.get(0).getUserEmail());
    }
}
