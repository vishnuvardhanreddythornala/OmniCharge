package com.omnicharge.operator.controller;


import com.omnicharge.operator.common.dto.ApiResponse;
import com.omnicharge.operator.service.SystemCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminSystemControllerTest {

    @Mock
    private SystemCacheService systemCacheService;

    @InjectMocks
    private AdminSystemController adminSystemController;

    @Test
    void rebuildCache() {
        doNothing().when(systemCacheService).rebuildRedisCache();
        
        ResponseEntity<ApiResponse<String>> response = adminSystemController.rebuildCache();
        
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        verify(systemCacheService, times(1)).rebuildRedisCache();
    }
}
