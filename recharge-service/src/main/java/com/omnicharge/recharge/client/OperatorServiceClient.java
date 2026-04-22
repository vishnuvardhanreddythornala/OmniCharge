package com.omnicharge.recharge.client;

import com.omnicharge.recharge.common.dto.ApiResponse;
import com.omnicharge.recharge.dto.PlanResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "operator-service")
public interface OperatorServiceClient {

    /**
     * Get plan details by ID with retry and caching
     * 
     * @Retry: Retries failed calls with exponential backoff (max 3 attempts)
     *      */
    @GetMapping("/api/plans/{id}")
    @Retry(name = "operatorService")
        ApiResponse<PlanResponse> getPlan(@PathVariable("id") Long id);
}
