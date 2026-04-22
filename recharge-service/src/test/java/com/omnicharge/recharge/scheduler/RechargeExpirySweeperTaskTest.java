package com.omnicharge.recharge.scheduler;

import com.omnicharge.recharge.common.dto.ApiResponse;
import com.omnicharge.recharge.common.event.RechargeCompletedEvent;
import com.omnicharge.recharge.common.logging.LogEvent;
import com.omnicharge.recharge.common.logging.LogEventPublisher;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.UserProfileResponse;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.repository.RechargeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class RechargeExpirySweeperTaskTest {

    @Mock
    private RechargeRepository rechargeRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private RechargeExpirySweeperTask sweeperTask;

    @Test
    void sweepExpiredRecharges_NoExpiredRecharges() {
        when(rechargeRepository.findByStatusAndPlanExpiryDateBefore(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        sweeperTask.sweepExpiredRecharges();

        verify(rechargeRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void sweepExpiredRecharges_Success() {
        Recharge r1 = new Recharge();
        r1.setRechargeId("REC1");
        r1.setUserId(1L);
        r1.setStatus(RechargeStatus.SUCCESS);
        r1.setPlanExpiryDate(LocalDate.now().minusDays(1));
        r1.setAmount(new BigDecimal("100"));

        Recharge r2 = new Recharge();
        r2.setRechargeId("REC2");
        r2.setUserId(2L);
        r2.setStatus(RechargeStatus.SUCCESS);
        r2.setPlanExpiryDate(LocalDate.now().minusDays(2));
        r2.setAmount(new BigDecimal("200"));

        when(rechargeRepository.findByStatusAndPlanExpiryDateBefore(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(r1, r2));

        UserProfileResponse u1 = new UserProfileResponse();
        u1.setEmail("u1@test.com");
        u1.setMobileNumber("999");
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.success(u1));

        // Let the second user fetch fail to test graceful degradation loop
        when(userServiceClient.getUserById(2L)).thenThrow(new RuntimeException("API error"));

        sweeperTask.sweepExpiredRecharges();

        verify(rechargeRepository, times(2)).save(any(Recharge.class));
        verify(rabbitTemplate, times(2)).convertAndSend(eq("omnicharge.exchange"), eq("plan.expiry"), any(RechargeCompletedEvent.class));
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void sweepExpiredRecharges_ExceptionInsideLoop() {
        Recharge r1 = new Recharge();
        r1.setRechargeId("REC1");
        r1.setUserId(1L);

        when(rechargeRepository.findByStatusAndPlanExpiryDateBefore(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(r1));
                
        when(rechargeRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        sweeperTask.sweepExpiredRecharges();

        // One loop iteration should fail, but not throw completely
        verify(rechargeRepository, times(1)).save(r1);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        
        // Log event should still fire context
        verify(logEventPublisher, times(1)).publish(argThat(evt -> evt.getLevel().equals("WARN")));
    }
}
