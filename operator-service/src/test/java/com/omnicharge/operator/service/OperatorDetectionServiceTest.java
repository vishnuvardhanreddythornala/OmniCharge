package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.common.logging.LogEvent;
import com.omnicharge.operator.common.logging.LogEventPublisher;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperatorDetectionServiceTest {

    @Mock private NumverifyClient numverifyClient;
    @Mock private OperatorRepository operatorRepository;
    @Mock private IPlanService planService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ObjectMapper objectMapper;
    @Mock private LogEventPublisher logEventPublisher;

    @InjectMocks
    private OperatorDetectionService operatorDetectionService;

    private Operator operator;
    private NumverifyResponse validNumResponse;

    @BeforeEach
    void setUp() {
        operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");
        operator.setCode("AIRTEL");

        validNumResponse = new NumverifyResponse();
        validNumResponse.setValid(true);
        validNumResponse.setCarrier("Bharti Airtel");
    }

    @Test
    void detectOperator_CacheHit() throws Exception {
        OperatorDetectionResponse cachedResp = OperatorDetectionResponse.builder().operatorName("Airtel").build();
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("operator:detect:9876543210")).thenReturn("json");
        when(objectMapper.readValue("json", OperatorDetectionResponse.class)).thenReturn(cachedResp);

        OperatorDetectionResponse result = operatorDetectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
        verify(numverifyClient, never()).detectOperator(anyString());
    }

    @Test
    void detectOperator_NumverifySuccess() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("operator:detect:9876543210")).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(validNumResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
        lenient().when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(operator));
        when(planService.getPlansByOperator(1L)).thenReturn(new ArrayList<>());

        OperatorDetectionResponse result = operatorDetectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
        verify(valueOperations, times(1)).set(eq("operator:detect:9876543210"), any(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void detectOperator_FallbackPrefixSuccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("operator:detect:9999543210")).thenReturn(null);
        
        NumverifyResponse invalidResponse = new NumverifyResponse();
        invalidResponse.setValid(false);
        when(numverifyClient.detectOperator("9999543210")).thenReturn(invalidResponse);
        
        Operator jio = new Operator();
        jio.setId(2L);
        jio.setName("Jio");
        
        when(operatorRepository.findByCode("JIO")).thenReturn(Optional.of(jio));
        when(planService.getPlansByOperator(2L)).thenReturn(new ArrayList<>());
        
        OperatorDetectionResponse result = operatorDetectionService.detectOperator("9999543210");
        
        assertNotNull(result);
        assertEquals(2L, result.getOperatorId());
    }

    @Test
    void detectOperator_FailedDetection() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("operator:detect:1111543210")).thenReturn(null);
        when(numverifyClient.detectOperator("1111543210")).thenReturn(null);
        when(operatorRepository.findByIsActive(true)).thenReturn(new ArrayList<>());

        OperatorDetectionResponse result = operatorDetectionService.detectOperator("1111543210");

        assertNull(result);
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void invalidateDetectionCacheForOperator_Success() {
        when(redisTemplate.keys("operator:detect:*")).thenReturn(Set.of("key1", "key2"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key1")).thenReturn("\"operatorId\":1,");
        when(valueOperations.get("key2")).thenReturn("\"operatorId\":2,");

        operatorDetectionService.invalidateDetectionCacheForOperator(1L);

        verify(redisTemplate, times(1)).delete("key1");
        verify(redisTemplate, never()).delete("key2");
    }

    @Test
    void detectOperator_MatchCarrierToOperatorVariants() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Test Vodafone / Idea / Vi branch
        NumverifyResponse viResp = new NumverifyResponse();
        viResp.setValid(true);
        viResp.setCarrier("Vodafone India");
        when(numverifyClient.detectOperator("9898000000")).thenReturn(viResp);
        
        Operator viOp = new Operator();
        viOp.setId(3L);
        viOp.setCode("VI");
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.of(viOp));
        when(planService.getPlansByOperator(3L)).thenReturn(new ArrayList<>());
        
        OperatorDetectionResponse result = operatorDetectionService.detectOperator("9898000000");
        assertNotNull(result);
        assertEquals(3L, result.getOperatorId());

        // Test BSNL branch
        NumverifyResponse bsnlResp = new NumverifyResponse();
        bsnlResp.setValid(true);
        bsnlResp.setCarrier("BSNL Mobile");
        when(numverifyClient.detectOperator("9444000000")).thenReturn(bsnlResp);
        
        Operator bsnlOp = new Operator();
        bsnlOp.setId(4L);
        bsnlOp.setCode("BSNL");
        when(operatorRepository.findByCode("BSNL")).thenReturn(Optional.of(bsnlOp));
        when(planService.getPlansByOperator(4L)).thenReturn(new ArrayList<>());
        
        result = operatorDetectionService.detectOperator("9444000000");
        assertNotNull(result);
        assertEquals(4L, result.getOperatorId());
    }

    @Test
    void detectOperator_CacheJsonProcessingException() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("operator:detect:9876543210")).thenReturn("invalid_json");
        when(objectMapper.readValue("invalid_json", OperatorDetectionResponse.class))
                .thenThrow(new JsonProcessingException("Parse Error") {});

        lenient().when(numverifyClient.detectOperator("9876543210")).thenReturn(null);
        lenient().when(operatorRepository.findByIsActive(true)).thenReturn(new ArrayList<>());

        OperatorDetectionResponse result = operatorDetectionService.detectOperator("9876543210");

        assertNull(result); // Falls through because of bad cache + null numverify + no active prefix
    }
}
