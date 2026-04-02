package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.client.NumverifyClient;
import com.omnicharge.operator.dto.NumverifyResponse;
import com.omnicharge.operator.dto.OperatorDetectionResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperatorDetectionServiceTest {

    @Mock
    private NumverifyClient numverifyClient;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private IPlanService planService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OperatorDetectionService detectionService;

    private Operator operator;
    private NumverifyResponse numverifyResponse;

    @BeforeEach
    void setUp() {
        operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");
        operator.setCode("AIRTEL");
        operator.setIsActive(true);

        numverifyResponse = new NumverifyResponse();
        numverifyResponse.setValid(true);
        numverifyResponse.setCarrier("Bharti Airtel Ltd");
    }

    @Test
    void detectOperator_CacheHit() throws Exception {
        String cacheKey = "operator:detect:9876543210";
        OperatorDetectionResponse cachedResponse = OperatorDetectionResponse.builder()
                .operatorCode("AIRTEL")
                .operatorName("Airtel")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn("{\"operatorCode\":\"AIRTEL\"}");
        when(objectMapper.readValue("{\"operatorCode\":\"AIRTEL\"}", OperatorDetectionResponse.class))
                .thenReturn(cachedResponse);

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("AIRTEL", result.getOperatorCode());
        verify(numverifyClient, never()).detectOperator(anyString());
    }

    @Test
    void detectOperator_NumverifySuccess() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
        lenient().when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals(1L, result.getOperatorId());
        assertEquals("Airtel", result.getOperatorName());
        verify(valueOperations, times(1)).set(eq("operator:detect:9876543210"), anyString(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void detectOperator_FallbackRegex() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        // Simulate API Failure / No Carrier Match
        numverifyResponse.setCarrier(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        
        // Target Regex (9876) matches AIRTEL fallback
        lenient().when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
    }
}
