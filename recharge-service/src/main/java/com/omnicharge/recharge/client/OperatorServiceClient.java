package com.omnicharge.recharge.client;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.recharge.dto.PlanResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "operator-service")
public interface OperatorServiceClient {

    /**
     * Get plan details by ID with retry and caching
     * 
     * @Retry: Retries failed calls with exponential backoff (max 3 attempts)
     * @Cacheable: Caches plan details for 1 hour to reduce load
     */
    @GetMapping("/api/plans/{id}")
    @Retry(name = "operatorService")
    @Cacheable(value = "planCache", key = "#id")
    ApiResponse<PlanResponse> getPlan(@PathVariable("id") Long id);
}
