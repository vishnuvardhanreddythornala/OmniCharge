package com.omnicharge.recharge.service;

import com.omnicharge.recharge.dto.ExpiringRechargeResponse;
import com.omnicharge.recharge.dto.RechargeRequest;
import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.dto.RechargeStatsResponse;
import com.omnicharge.recharge.entity.RechargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface IRechargeService {

    RechargeResponse initiateRecharge(Long userId, RechargeRequest request);

    RechargeResponse getRechargeById(String rechargeId, Long userId);

    Page<RechargeResponse> getRechargeHistory(Long userId, Pageable pageable);

    /** Date-filtered user recharge history */
    Page<RechargeResponse> getRechargeHistory(Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    String getRechargeStatus(String rechargeId);

    Page<RechargeResponse> getAllRecharges(Pageable pageable);

    /** Date-filtered admin-level recharge listing */
    Page<RechargeResponse> getAllRecharges(String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    RechargeStatsResponse getRechargeStats();

    List<ExpiringRechargeResponse> getExpiringRecharges(int daysLeft);

    List<ExpiringRechargeResponse> getExpiredToday();

    void markAsExpired(String rechargeId);
}
