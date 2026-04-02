package com.omnicharge.notification.scheduler;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.notification.client.RechargeServiceClient;
import com.omnicharge.notification.dto.ExpiringRechargeResponse;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.service.IEmailService;
import com.omnicharge.notification.service.INotificationService;
import com.omnicharge.notification.service.ISmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanExpirySchedulerTest {

    @Mock private RechargeServiceClient rechargeServiceClient;
    @Mock private IEmailService emailService;
    @Mock private ISmsService smsService;
    @Mock private INotificationService notificationService;

    @InjectMocks
    private PlanExpiryScheduler planExpiryScheduler;

    private ExpiringRechargeResponse buildExpiring(String email, String mobile) {
        return ExpiringRechargeResponse.builder()
                .rechargeId("REC-1").userId(1L).userEmail(email).userMobile(mobile)
                .mobileNumber("9876543210").operatorName("Jio").planName("5G Plan")
                .amount(new BigDecimal("299")).expiryDate(LocalDate.now().plusDays(5)).build();
    }

    // === checkPlanExpiries (Expiring) ===

    @Test
    void checkPlanExpiries_ExpiringPlans_SendsReminders() {
        ExpiringRechargeResponse recharge = buildExpiring("u@t.com", "9876543210");
        ApiResponse<List<ExpiringRechargeResponse>> response = new ApiResponse<>(true, "ok",
                Collections.singletonList(recharge), LocalDateTime.now());
        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(response);
        when(rechargeServiceClient.getExpiredToday()).thenReturn(
                new ApiResponse<>(true, "ok", Collections.emptyList(), LocalDateTime.now()));

        planExpiryScheduler.checkPlanExpiries();

        verify(emailService).sendPlanExpiryReminder("u@t.com", "User", "Jio", "5G Plan", "9876543210", 5);
        verify(notificationService).createAndSendEmail(eq(1L), eq("u@t.com"), eq("Plan Expiry Reminder"),
                anyString(), eq(NotificationCategory.PLAN_EXPIRY_REMINDER), eq("REC-1"));
        verify(smsService).sendSms(eq("9876543210"), contains("expires in 5 days"));
    }

    @Test
    void checkPlanExpiries_NoExpiringPlans_SkipsReminders() {
        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(
                new ApiResponse<>(true, "ok", Collections.emptyList(), LocalDateTime.now()));
        when(rechargeServiceClient.getExpiredToday()).thenReturn(
                new ApiResponse<>(true, "ok", Collections.emptyList(), LocalDateTime.now()));

        planExpiryScheduler.checkPlanExpiries();

        verify(emailService, never()).sendPlanExpiryReminder(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt());
    }

    @Test
    void checkPlanExpiries_NullDataFromApi() {
        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(
                new ApiResponse<>(true, "ok", null, LocalDateTime.now()));
        when(rechargeServiceClient.getExpiredToday()).thenReturn(
                new ApiResponse<>(true, "ok", null, LocalDateTime.now()));

        planExpiryScheduler.checkPlanExpiries();

        verify(emailService, never()).sendPlanExpiryReminder(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt());
    }

    @Test
    void checkPlanExpiries_ExpiringNoEmail_SkipsEmail() {
        ExpiringRechargeResponse recharge = buildExpiring(null, "9876543210");
        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(
                new ApiResponse<>(true, "ok", Collections.singletonList(recharge), LocalDateTime.now()));
        when(rechargeServiceClient.getExpiredToday()).thenReturn(
                new ApiResponse<>(true, "ok", Collections.emptyList(), LocalDateTime.now()));

        planExpiryScheduler.checkPlanExpiries();

        verify(emailService, never()).sendPlanExpiryReminder(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt());
        verify(smsService).sendSms(eq("9876543210"), anyString());
    }

    @Test
    void checkPlanExpiries_ExpiringNoMobile_SkipsSms() {
        ExpiringRechargeResponse recharge = buildExpiring("u@t.com", null);
        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(
                new ApiResponse<>(true, "ok", Collections.singletonList(recharge), LocalDateTime.now()));
        when(rechargeServiceClient.getExpiredToday()).thenReturn(
                new ApiResponse<>(true, "ok", Collections.emptyList(), LocalDateTime.now()));

        planExpiryScheduler.checkPlanExpiries();

        verify(emailService).sendPlanExpiryReminder(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt());
        verify(smsService, never()).sendSms(anyString(), anyString());
    }

    // === checkPlanExpiries (Expired) ===

    @Test
    void checkPlanExpiries_ExpiredPlans_SendsNotificationsAndMarksExpired() {
        ExpiringRechargeResponse recharge = buildExpiring("u@t.com", "9876543210");
        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(
                new ApiResponse<>(true, "ok", Collections.emptyList(), LocalDateTime.now()));
        when(rechargeServiceClient.getExpiredToday()).thenReturn(
                new ApiResponse<>(true, "ok", Collections.singletonList(recharge), LocalDateTime.now()));

        planExpiryScheduler.checkPlanExpiries();

        verify(emailService).sendPlanExpiredNotification("u@t.com", "User", "Jio", "5G Plan", "9876543210");
        verify(notificationService).createAndSendEmail(eq(1L), eq("u@t.com"), eq("Plan Expired"),
                anyString(), eq(NotificationCategory.PLAN_EXPIRED), eq("REC-1"));
        verify(smsService).sendSms(eq("9876543210"), contains("has expired"));
        verify(rechargeServiceClient).markAsExpired("REC-1");
    }

    @Test
    void checkPlanExpiries_ExpiredNoEmail_SkipsEmail() {
        ExpiringRechargeResponse recharge = buildExpiring(null, "9876543210");
        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(
                new ApiResponse<>(true, "ok", Collections.emptyList(), LocalDateTime.now()));
        when(rechargeServiceClient.getExpiredToday()).thenReturn(
                new ApiResponse<>(true, "ok", Collections.singletonList(recharge), LocalDateTime.now()));

        planExpiryScheduler.checkPlanExpiries();

        verify(emailService, never()).sendPlanExpiredNotification(anyString(), anyString(), anyString(),
                anyString(), anyString());
        verify(rechargeServiceClient).markAsExpired("REC-1");
    }

    @Test
    void checkPlanExpiries_FeignClientThrows_HandlesGracefully() {
        when(rechargeServiceClient.getExpiringRecharges(5)).thenThrow(new RuntimeException("Feign timeout"));
        when(rechargeServiceClient.getExpiredToday()).thenReturn(
                new ApiResponse<>(true, "ok", Collections.emptyList(), LocalDateTime.now()));

        planExpiryScheduler.checkPlanExpiries(); // Should not throw

        verify(emailService, never()).sendPlanExpiryReminder(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt());
    }

    @Test
    void checkPlanExpiries_MarkAsExpired_Failure_HandlesGracefully() {
        ExpiringRechargeResponse recharge = buildExpiring("u@t.com", "9876543210");
        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(
                new ApiResponse<>(true, "ok", Collections.emptyList(), LocalDateTime.now()));
        when(rechargeServiceClient.getExpiredToday()).thenReturn(
                new ApiResponse<>(true, "ok", Collections.singletonList(recharge), LocalDateTime.now()));
        doThrow(new RuntimeException("Feign error")).when(rechargeServiceClient).markAsExpired(anyString());

        planExpiryScheduler.checkPlanExpiries(); // Should not throw

        verify(emailService).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void checkPlanExpiries_MultipleExpiring_IteratesAll() {
        ExpiringRechargeResponse r1 = buildExpiring("a@t.com", "111");
        ExpiringRechargeResponse r2 = buildExpiring("b@t.com", "222");
        when(rechargeServiceClient.getExpiringRecharges(5)).thenReturn(
                new ApiResponse<>(true, "ok", Arrays.asList(r1, r2), LocalDateTime.now()));
        when(rechargeServiceClient.getExpiredToday()).thenReturn(
                new ApiResponse<>(true, "ok", Collections.emptyList(), LocalDateTime.now()));

        planExpiryScheduler.checkPlanExpiries();

        verify(emailService, times(2)).sendPlanExpiryReminder(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt());
    }
}
