package com.omnicharge.recharge.service;

import com.omnicharge.recharge.common.dto.ApiResponse;
import com.omnicharge.recharge.common.exception.BadRequestException;
import com.omnicharge.recharge.common.exception.ResourceNotFoundException;
import com.omnicharge.recharge.common.logging.LogEvent;
import com.omnicharge.recharge.common.logging.LogEventPublisher;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private RechargeService rechargeService;

    private RechargeRequest validRequest;
    private PlanResponse validPlan;

    @BeforeEach
    void setUp() {
        validRequest = new RechargeRequest();
        validRequest.setOperatorId(1L);
        validRequest.setPlanId(10L);
        validRequest.setMobileNumber("9876543210");
        validRequest.setPaymentMethod("UPI");

        validPlan = new PlanResponse();
        validPlan.setId(10L);
        validPlan.setOperatorId(1L);
        validPlan.setOperatorName("Airtel");
        validPlan.setPlanName("Basic 199");
        validPlan.setPrice(new BigDecimal("199.00"));
        validPlan.setValidityDays(28);
        validPlan.setIsActive(true);
    }

    @Test
    void initiateRecharge_Success() {
        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.success(validPlan));
        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse userProfile = new UserProfileResponse();
        userProfile.setEmail("user@example.com");
        userProfile.setIsEmailVerified(true);
        userProfile.setMobileNumber("9876543210");
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.success(userProfile));

        RechargeResponse response = rechargeService.initiateRecharge(1L, validRequest);

        assertNotNull(response);
        assertEquals(RechargeStatus.PROCESSING, response.getStatus()); // Saves INITIATED then PROCESSING
        verify(rechargeEventProducer, times(1)).publishRechargeInitiated(any());
        verify(logEventPublisher, atLeastOnce()).publish(any(LogEvent.class));
        verify(rechargeRepository, times(2)).save(any());
    }

    @Test
    void initiateRecharge_OperatorServiceCircuitBreakerFails() {
        when(operatorServiceClient.getPlan(10L)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            rechargeService.initiateRecharge(1L, validRequest);
        });

        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.error("Service Down"));

        assertThrows(BadRequestException.class, () -> {
            rechargeService.initiateRecharge(1L, validRequest);
        });
        
        verify(rechargeRepository, never()).save(any());
    }

    @Test
    void initiateRecharge_PlanInactive() {
        validPlan.setIsActive(false);
        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.success(validPlan));

        assertThrows(BadRequestException.class, () -> {
            rechargeService.initiateRecharge(1L, validRequest);
        });
        verify(rechargeRepository, never()).save(any());
    }

    @Test
    void initiateRecharge_PlanOperatorMismatch() {
        validRequest.setOperatorId(2L); // Different operator ID parameter
        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.success(validPlan));

        assertThrows(BadRequestException.class, () -> {
            rechargeService.initiateRecharge(1L, validRequest);
        });
        verify(rechargeRepository, never()).save(any());
    }
    
    @Test
    void initiateRecharge_UserServiceFailsSilent() {
        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.success(validPlan));
        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(i -> i.getArgument(0));
        
        // Mock fail to swallow the exception (graceful degradation)
        when(userServiceClient.getUserById(1L)).thenThrow(new RuntimeException("API Down"));

        RechargeResponse response = rechargeService.initiateRecharge(1L, validRequest);

        assertNotNull(response);
    }

    @Test
    void getRechargeById_Success() {
        Recharge recharge = new Recharge();
        recharge.setRechargeId("REC-123");
        recharge.setUserId(1L);

        when(rechargeRepository.findByRechargeId("REC-123")).thenReturn(Optional.of(recharge));

        RechargeResponse response = rechargeService.getRechargeById("REC-123", 1L);

        assertNotNull(response);
        assertEquals("REC-123", response.getRechargeId());
    }

    @Test
    void getRechargeById_Unauthorized() {
        Recharge recharge = new Recharge();
        recharge.setRechargeId("REC-123");
        recharge.setUserId(2L);

        when(rechargeRepository.findByRechargeId("REC-123")).thenReturn(Optional.of(recharge));

        assertThrows(BadRequestException.class, () -> {
            rechargeService.getRechargeById("REC-123", 1L);
        });
    }

    @Test
    void markAsExpired_Success() {
        Recharge recharge = new Recharge();
        recharge.setRechargeId("REC-123");
        recharge.setUserId(1L);
        recharge.setPlanExpiryDate(LocalDate.now().minusDays(1));

        when(rechargeRepository.findByRechargeId("REC-123")).thenReturn(Optional.of(recharge));

        rechargeService.markAsExpired("REC-123");

        assertEquals(RechargeStatus.EXPIRED, recharge.getStatus());
        verify(rechargeRepository, times(1)).save(recharge);
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void markAsExpired_NotFound() {
        when(rechargeRepository.findByRechargeId("REC-123")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> rechargeService.markAsExpired("REC-123"));
    }

    @Test
    void getRechargeHistory_Success() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getRechargeHistory(1L, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }
    
    @Test
    void getRechargeHistory_WithDateFilters_Success() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findByUserIdWithDateFilters(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getRechargeHistory(1L, null, null, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getRechargeStatus_Success() {
        Recharge recharge = new Recharge();
        recharge.setStatus(RechargeStatus.SUCCESS);
        when(rechargeRepository.findByRechargeId("REC-123")).thenReturn(Optional.of(recharge));
        
        String status = rechargeService.getRechargeStatus("REC-123");
        assertEquals("SUCCESS", status);
    }

    @Test
    void getRechargeStatus_NotFound() {
        when(rechargeRepository.findByRechargeId("REC-123")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> rechargeService.getRechargeStatus("REC-123"));
    }

    @Test
    void getAllRecharges_NoStatus_Success() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        
        when(rechargeRepository.findAllWithDateFilters(any(), any(), any())).thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getAllRecharges("", null, null, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getAllRecharges_Processing_Success() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        
        when(rechargeRepository.findAllWithStatusesAndDateFilters(anyList(), any(), any(), any())).thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getAllRecharges("PROCESSING", null, null, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getAllRecharges_SuccessStatus_Success() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        
        when(rechargeRepository.findAllWithStatusAndDateFilters(eq(RechargeStatus.SUCCESS), any(), any(), any())).thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getAllRecharges("SUCCESS", null, null, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getAllRecharges_Simple_Success() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findAll(any(Pageable.class))).thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getAllRecharges(Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getRechargeStats_Success() {
        when(rechargeRepository.count()).thenReturn(50L);
        when(rechargeRepository.countByStatus(RechargeStatus.SUCCESS)).thenReturn(40L);
        when(rechargeRepository.countByStatus(RechargeStatus.FAILED)).thenReturn(10L);
        
        Recharge successMatch = new Recharge();
        successMatch.setStatus(RechargeStatus.SUCCESS);
        successMatch.setAmount(new BigDecimal("100"));
        
        Recharge failMatch = new Recharge();
        failMatch.setStatus(RechargeStatus.FAILED);
        failMatch.setAmount(new BigDecimal("100"));

        when(rechargeRepository.findByCreatedDateBetween(any(), any())).thenReturn(List.of(successMatch, failMatch));
        
        RechargeStatsResponse response = rechargeService.getRechargeStats();
        assertNotNull(response);
        assertEquals(50L, response.getTotalRecharges());
        assertEquals(new BigDecimal("100"), response.getTotalAmount());
    }

    @Test
    void getExpiringRecharges_Success() {
        Recharge recharge = new Recharge();
        recharge.setUserId(1L);
        recharge.setAmount(new BigDecimal("100"));
        
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(recharge));
                
        UserProfileResponse user = new UserProfileResponse();
        user.setEmail("test@test.com");
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.success(user));
        
        List<ExpiringRechargeResponse> responses = rechargeService.getExpiringRecharges(3);
        assertEquals(1, responses.size());
        assertEquals("test@test.com", responses.get(0).getUserEmail());
    }

    @Test
    void getExpiringRecharges_UserFetchThrows() {
        Recharge recharge = new Recharge();
        recharge.setUserId(1L);
        
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(recharge));
                
        when(userServiceClient.getUserById(1L)).thenThrow(new RuntimeException("Unable to fetch user"));
        
        List<ExpiringRechargeResponse> responses = rechargeService.getExpiringRecharges(3);
        assertEquals(1, responses.size());
        assertNull(responses.get(0).getUserEmail());
    }

    @Test
    void getExpiredToday_Success() {
        Recharge recharge = new Recharge();
        recharge.setUserId(1L);
        
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(recharge));
                
        List<ExpiringRechargeResponse> responses = rechargeService.getExpiredToday();
        assertEquals(1, responses.size());
    }

    @Test
    void initiateRecharge_NullRequest() {
        assertThrows(NullPointerException.class, () -> {
            rechargeService.initiateRecharge(1L, null);
        });
    }

    @Test
    void initiateRecharge_EmptyMobileNumber() {
        validRequest.setMobileNumber("");
        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.success(validPlan));
        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(i -> i.getArgument(0));

        RechargeResponse response = rechargeService.initiateRecharge(1L, validRequest);
        assertNotNull(response);
        assertEquals("", response.getMobileNumber());
    }

    @Test
    void initiateRecharge_NullMobileNumber() {
        validRequest.setMobileNumber(null);
        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.success(validPlan));
        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(i -> i.getArgument(0));

        RechargeResponse response = rechargeService.initiateRecharge(1L, validRequest);
        assertNotNull(response);
        assertNull(response.getMobileNumber());
    }

    @Test
    void getAllRecharges_InitiatedStatus_Success() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        
        when(rechargeRepository.findAllWithStatusesAndDateFilters(anyList(), any(), any(), any())).thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getAllRecharges("INITIATED", null, null, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getAllRecharges_FailedStatus_Success() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        
        when(rechargeRepository.findAllWithStatusAndDateFilters(eq(RechargeStatus.FAILED), any(), any(), any())).thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getAllRecharges("FAILED", null, null, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getAllRecharges_ExpiredStatus_Success() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        
        when(rechargeRepository.findAllWithStatusAndDateFilters(eq(RechargeStatus.EXPIRED), any(), any(), any())).thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getAllRecharges("EXPIRED", null, null, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getAllRecharges_NullStatus_Success() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        
        when(rechargeRepository.findAllWithDateFilters(any(), any(), any())).thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getAllRecharges(null, null, null, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getRechargeHistory_WithNullStartDate() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findByUserIdWithDateFilters(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getRechargeHistory(1L, null, LocalDateTime.now(), Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getRechargeHistory_WithNullEndDate() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findByUserIdWithDateFilters(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getRechargeHistory(1L, LocalDateTime.now(), null, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getRechargeHistory_WithBothDatesNull() {
        Recharge recharge = new Recharge();
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findByUserIdWithDateFilters(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);
        
        Page<RechargeResponse> response = rechargeService.getRechargeHistory(1L, null, null, Pageable.unpaged());
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getExpiringRecharges_EmptyList() {
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
                
        List<ExpiringRechargeResponse> responses = rechargeService.getExpiringRecharges(3);
        assertEquals(0, responses.size());
    }

    @Test
    void getExpiredToday_EmptyList() {
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
                
        List<ExpiringRechargeResponse> responses = rechargeService.getExpiredToday();
        assertEquals(0, responses.size());
    }

    @Test
    void getRechargeStats_NoSuccessfulRecharges() {
        when(rechargeRepository.count()).thenReturn(10L);
        when(rechargeRepository.countByStatus(RechargeStatus.SUCCESS)).thenReturn(0L);
        when(rechargeRepository.countByStatus(RechargeStatus.FAILED)).thenReturn(10L);
        when(rechargeRepository.findByCreatedDateBetween(any(), any())).thenReturn(Collections.emptyList());
        
        RechargeStatsResponse response = rechargeService.getRechargeStats();
        assertNotNull(response);
        assertEquals(10L, response.getTotalRecharges());
        assertEquals(BigDecimal.ZERO, response.getTotalAmount());
    }

    @Test
    void getRechargeStats_MixedStatuses() {
        when(rechargeRepository.count()).thenReturn(100L);
        when(rechargeRepository.countByStatus(RechargeStatus.SUCCESS)).thenReturn(80L);
        when(rechargeRepository.countByStatus(RechargeStatus.FAILED)).thenReturn(20L);
        
        Recharge success1 = new Recharge();
        success1.setStatus(RechargeStatus.SUCCESS);
        success1.setAmount(new BigDecimal("100"));
        
        Recharge success2 = new Recharge();
        success2.setStatus(RechargeStatus.SUCCESS);
        success2.setAmount(new BigDecimal("200"));
        
        Recharge failed = new Recharge();
        failed.setStatus(RechargeStatus.FAILED);
        failed.setAmount(new BigDecimal("50"));

        when(rechargeRepository.findByCreatedDateBetween(any(), any())).thenReturn(List.of(success1, success2, failed));
        
        RechargeStatsResponse response = rechargeService.getRechargeStats();
        assertNotNull(response);
        assertEquals(100L, response.getTotalRecharges());
        assertEquals(new BigDecimal("300"), response.getTotalAmount());
    }

    @Test
    void initiateRecharge_UserServiceReturnsNull() {
        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.success(validPlan));
        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(i -> i.getArgument(0));
        when(userServiceClient.getUserById(1L)).thenReturn(null);

        RechargeResponse response = rechargeService.initiateRecharge(1L, validRequest);
        assertNotNull(response);
    }

    @Test
    void initiateRecharge_UserServiceReturnsError() {
        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.success(validPlan));
        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(i -> i.getArgument(0));
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.error("User service error"));

        RechargeResponse response = rechargeService.initiateRecharge(1L, validRequest);
        assertNotNull(response);
    }

    @Test
    void initiateRecharge_UserServiceReturnsNullData() {
        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.success(validPlan));
        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(i -> i.getArgument(0));
        
        ApiResponse<UserProfileResponse> nullDataResponse = new ApiResponse<>();
        nullDataResponse.setSuccess(true);
        nullDataResponse.setData(null);
        when(userServiceClient.getUserById(1L)).thenReturn(nullDataResponse);

        RechargeResponse response = rechargeService.initiateRecharge(1L, validRequest);
        assertNotNull(response);
    }

    @Test
    void initiateRecharge_UserEmailNotVerified() {
        when(operatorServiceClient.getPlan(10L)).thenReturn(ApiResponse.success(validPlan));
        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse userProfile = new UserProfileResponse();
        userProfile.setEmail("user@example.com");
        userProfile.setIsEmailVerified(false);
        userProfile.setMobileNumber("9876543210");
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.success(userProfile));

        RechargeResponse response = rechargeService.initiateRecharge(1L, validRequest);
        assertNotNull(response);
    }

    @Test
    void mapToExpiringResponse_UserServiceReturnsNullData() {
        Recharge recharge = new Recharge();
        recharge.setUserId(1L);
        recharge.setRechargeId("REC-123");
        recharge.setAmount(new BigDecimal("100"));
        
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(recharge));
        
        ApiResponse<UserProfileResponse> nullDataResponse = new ApiResponse<>();
        nullDataResponse.setSuccess(true);
        nullDataResponse.setData(null);
        when(userServiceClient.getUserById(1L)).thenReturn(nullDataResponse);
        
        List<ExpiringRechargeResponse> responses = rechargeService.getExpiringRecharges(3);
        assertEquals(1, responses.size());
        assertNull(responses.get(0).getUserEmail());
    }

    @Test
    void mapToExpiringResponse_UserServiceReturnsError() {
        Recharge recharge = new Recharge();
        recharge.setUserId(1L);
        recharge.setRechargeId("REC-123");
        
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(recharge));
        
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.error("Service error"));
        
        List<ExpiringRechargeResponse> responses = rechargeService.getExpiringRecharges(3);
        assertEquals(1, responses.size());
        assertNull(responses.get(0).getUserEmail());
    }
}
