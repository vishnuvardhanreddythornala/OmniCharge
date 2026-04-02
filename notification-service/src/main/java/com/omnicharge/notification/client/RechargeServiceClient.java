package com.omnicharge.notification.client;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.notification.dto.ExpiringRechargeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "recharge-service")
public interface RechargeServiceClient {

    @GetMapping("/api/internal/recharges/expiring")
    ApiResponse<List<ExpiringRechargeResponse>> getExpiringRecharges(@RequestParam("daysLeft") int daysLeft);

    @GetMapping("/api/internal/recharges/expired-today")
    ApiResponse<List<ExpiringRechargeResponse>> getExpiredToday();

    @PutMapping("/api/internal/recharges/{rechargeId}/expire")
    ApiResponse<Void> markAsExpired(@PathVariable("rechargeId") String rechargeId);
}
