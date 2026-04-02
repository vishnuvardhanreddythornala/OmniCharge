package com.omnicharge.operator.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.operator.service.SystemCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final SystemCacheService systemCacheService;

    @PostMapping("/rebuild-cache")
    public ResponseEntity<ApiResponse<String>> rebuildCache() {
        systemCacheService.rebuildRedisCache();
        return ResponseEntity.ok(ApiResponse.success("Redis cache rebuild initiated and completed successfully", null));
    }
}
