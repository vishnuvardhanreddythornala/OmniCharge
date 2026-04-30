package com.omnicharge.payment.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "recharge-service", path = "/api/internal/recharges")
public interface RechargeServiceClient {

    @GetMapping("/{rechargeId}")
    @CircuitBreaker(name = "rechargeService", fallbackMethod = "getRechargeFallback")
    @Retry(name = "rechargeService")
    ResponseEntity<Map<String, Object>> getRechargeById(@PathVariable("rechargeId") String rechargeId);

    default ResponseEntity<Map<String, Object>> getRechargeFallback(String rechargeId, Throwable t) {
        return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Recharge service is currently unavailable. Fallback executed."
        ));
    }
}
