package com.omnicharge.recharge.service;

import com.omnicharge.recharge.dto.ExpiringRechargeResponse;
import com.omnicharge.recharge.dto.RechargeRequest;
import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.dto.RechargeStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IRechargeService {

    RechargeResponse initiateRecharge(Long userId, RechargeRequest request);

    RechargeResponse getRechargeById(String rechargeId, Long userId);

    Page<RechargeResponse> getRechargeHistory(Long userId, Pageable pageable);

    String getRechargeStatus(String rechargeId);

    Page<RechargeResponse> getAllRecharges(Pageable pageable);

    RechargeStatsResponse getRechargeStats();

    List<ExpiringRechargeResponse> getExpiringRecharges(int daysLeft);

    List<ExpiringRechargeResponse> getExpiredToday();

    void markAsExpired(String rechargeId);
}
