package com.omnicharge.notification.scheduler;

import com.omnicharge.notification.client.RechargeServiceClient;
import com.omnicharge.notification.common.dto.ApiResponse;
import com.omnicharge.notification.dto.ExpiringRechargeResponse;
import com.omnicharge.notification.service.IEmailService;
import com.omnicharge.notification.service.INotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class PlanExpirySchedulerTest {

    @Mock private RechargeServiceClient rechargeServiceClient;
    @Mock private IEmailService emailService;
    @Mock private INotificationService notificationService;
    @InjectMocks private PlanExpiryScheduler scheduler;

    @Test
    void checkPlanExpiries_WithExpiringPlans() {
        ExpiringRechargeResponse recharge = new ExpiringRechargeResponse();
        recharge.setRechargeId("REC-123");
        recharge.setUserId(1L);
        recharge.setUserEmail("user@test.com");
        recharge.setMobileNumber("9876543210");
        recharge.setOperatorName("Jio");
        recharge.setPlanName("Gold");

        ApiResponse<List<ExpiringRechargeResponse>> response = new ApiResponse<>();
        response.setData(List.of(recharge));

        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(response);

        scheduler.checkPlanExpiries();

        verify(emailService, atLeastOnce()).sendPlanExpiryReminder(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void checkPlanExpiries_NoExpiringPlans() {
        ApiResponse<List<ExpiringRechargeResponse>> response = new ApiResponse<>();
        response.setData(Collections.emptyList());

        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(response);

        scheduler.checkPlanExpiries();

        verify(emailService, never()).sendPlanExpiryReminder(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void checkPlanExpiries_NullData() {
        ApiResponse<List<ExpiringRechargeResponse>> response = new ApiResponse<>();
        response.setData(null);

        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(response);

        scheduler.checkPlanExpiries();

        verify(emailService, never()).sendPlanExpiryReminder(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void checkPlanExpiries_ApiException() {
        when(rechargeServiceClient.getExpiringRecharges(5)).thenThrow(new RuntimeException("Feign error"));

        scheduler.checkPlanExpiries();

        verify(emailService, never()).sendPlanExpiryReminder(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void checkPlanExpiries_NoEmail() {
        ExpiringRechargeResponse recharge = new ExpiringRechargeResponse();
        recharge.setRechargeId("REC-123");
        recharge.setUserId(1L);
        recharge.setUserEmail(null);

        ApiResponse<List<ExpiringRechargeResponse>> response = new ApiResponse<>();
        response.setData(List.of(recharge));

        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(response);

        scheduler.checkPlanExpiries();

        verify(emailService, never()).sendPlanExpiryReminder(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }
}
