package com.omnicharge.operator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.client.NumverifyClient;
import com.omnicharge.operator.dto.NumverifyResponse;
import com.omnicharge.operator.dto.OperatorDetectionResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for OperatorDetectionService business operation logging.
 * 
 * Validates Property 33: Business Operation Event Logging
 * "For any operator detection operation, the system should log the event with
 * relevant business context including phone number, detected operator, and outcome."
 * 
 * This test verifies that operator detection operations publish log events
 * with appropriate business context.
 */
@ExtendWith(MockitoExtension.class)
@Tag("Feature: production-grade-centralized-logging, Property 33: Business Operation Event Logging")
class OperatorDetectionServiceBusinessOperationPropertyTest {

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

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private OperatorDetectionService detectionService;

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
    }

    @Test
    void property_operatorDetectionSuccess_shouldLogWithBusinessContext() throws Exception {
        // Property: Successful operator detection must log with phone number, operator details, and outcome
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            String phoneNumber = generateRandomPhoneNumber();
            Operator operator = createRandomOperator();
            NumverifyResponse numverifyResponse = createNumverifyResponse(operator.getName());
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null); // Cache miss
            when(numverifyClient.detectOperator(phoneNumber)).thenReturn(numverifyResponse);
            when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
            lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            OperatorDetectionResponse result = detectionService.detectOperator(phoneNumber);
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("OPERATOR_DETECTION");
            assertThat(capturedEvent.getMessage()).contains("Operator detected");
            assertThat(capturedEvent.getMessage()).contains("mobile=" + phoneNumber);
            
            // Verify business context
            Map<String, Object> context = capturedEvent.getContext();
            assertThat(context).containsKey("mobileNumber");
            assertThat(context).containsKey("operatorId");
            assertThat(context).containsKey("operatorCode");
            assertThat(context).containsKey("operatorName");
            assertThat(context).containsEntry("detectionResult", "SUCCESS");
            assertThat(context.get("mobileNumber")).isEqualTo(phoneNumber);
            
            // Reset mocks
            reset(logEventPublisher, numverifyClient, operatorRepository, redisTemplate);
        }
    }

    @Test
    void property_operatorDetectionFailure_shouldLogWithFailureContext() {
        // Property: Failed operator detection must log with phone number, failure reason, and outcome
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            String phoneNumber = generateRandomPhoneNumber();
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null); // Cache miss
            when(numverifyClient.detectOperator(phoneNumber)).thenReturn(null); // Numverify fails
            when(operatorRepository.findByIsActive(true)).thenReturn(List.of()); // No operators in DB
            when(operatorRepository.findByCode(anyString())).thenReturn(Optional.empty()); // No prefix match
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            OperatorDetectionResponse result = detectionService.detectOperator(phoneNumber);
            
            // Assert
            assertThat(result).isNull(); // Detection failed
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("OPERATOR_DETECTION");
            assertThat(capturedEvent.getMessage()).contains("Operator detection failed");
            assertThat(capturedEvent.getMessage()).contains("mobile=" + phoneNumber);
            
            // Verify business context
            Map<String, Object> context = capturedEvent.getContext();
            assertThat(context).containsKey("mobileNumber");
            assertThat(context).containsEntry("detectionResult", "FAILED");
            assertThat(context).containsKey("reason");
            assertThat(context.get("mobileNumber")).isEqualTo(phoneNumber);
            
            // Reset mocks
            reset(logEventPublisher, numverifyClient, operatorRepository, redisTemplate);
        }
    }

    @Test
    void property_allDetectionLogs_shouldContainPhoneNumber() throws Exception {
        // Property: All operator detection logs must contain phone number
        // Run 100+ iterations
        
        for (int i = 0; i < 100; i++) {
            String phoneNumber = generateRandomPhoneNumber();
            Operator operator = createRandomOperator();
            NumverifyResponse numverifyResponse = createNumverifyResponse(operator.getName());
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(numverifyClient.detectOperator(phoneNumber)).thenReturn(numverifyResponse);
            when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
            lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            detectionService.detectOperator(phoneNumber);
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent event = logEventCaptor.getValue();
            Map<String, Object> context = event.getContext();
            assertThat(context).containsKey("mobileNumber");
            assertThat(context.get("mobileNumber")).isEqualTo(phoneNumber);
            assertThat(event.getServiceName()).isEqualTo("operator-service");
            assertThat(event.getTimestamp()).isNotNull();
            
            // Reset mocks
            reset(logEventPublisher, numverifyClient, operatorRepository, redisTemplate);
        }
    }

    @Test
    void property_allDetectionLogs_shouldContainOutcome() throws Exception {
        // Property: All operator detection logs must contain outcome (SUCCESS/FAILURE)
        // Run 100+ iterations with mixed success/failure scenarios
        
        for (int i = 0; i < 100; i++) {
            String phoneNumber = generateRandomPhoneNumber();
            boolean shouldSucceed = random.nextBoolean();
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            if (shouldSucceed) {
                Operator operator = createRandomOperator();
                NumverifyResponse numverifyResponse = createNumverifyResponse(operator.getName());
                when(numverifyClient.detectOperator(phoneNumber)).thenReturn(numverifyResponse);
                when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
                lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");
                
                detectionService.detectOperator(phoneNumber);
            } else {
                when(numverifyClient.detectOperator(phoneNumber)).thenReturn(null); // Numverify fails
                when(operatorRepository.findByIsActive(true)).thenReturn(List.of()); // No operators
                when(operatorRepository.findByCode(anyString())).thenReturn(Optional.empty()); // No prefix match
                
                detectionService.detectOperator(phoneNumber);
            }
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent event = logEventCaptor.getValue();
            Map<String, Object> context = event.getContext();
            assertThat(context).containsKey("detectionResult");
            
            String outcome = (String) context.get("detectionResult");
            assertThat(outcome).isIn("SUCCESS", "FAILED");
            
            // Reset mocks
            reset(logEventPublisher, numverifyClient, operatorRepository, redisTemplate);
        }
    }

    // Helper methods to generate random test data
    
    private String generateRandomPhoneNumber() {
        // Generate Indian phone numbers (10 digits starting with 6-9)
        int firstDigit = 6 + random.nextInt(4); // 6, 7, 8, or 9
        long remainingDigits = random.nextLong(100000000, 999999999);
        return String.valueOf(firstDigit) + remainingDigits;
    }
    
    private Operator createRandomOperator() {
        Operator operator = new Operator();
        operator.setId(random.nextLong(1, 10000));
        operator.setName("Operator " + random.nextInt(1000));
        operator.setCode("OP" + random.nextInt(1000));
        operator.setCategory(getRandomOperatorCategory());
        operator.setIsActive(true);
        return operator;
    }
    
    private NumverifyResponse createNumverifyResponse(String carrierName) {
        NumverifyResponse response = new NumverifyResponse();
        response.setValid(true);
        response.setCarrier(carrierName);
        response.setCountryCode("IN");
        return response;
    }
    
    private com.omnicharge.operator.entity.OperatorCategory getRandomOperatorCategory() {
        com.omnicharge.operator.entity.OperatorCategory[] categories = com.omnicharge.operator.entity.OperatorCategory.values();
        return categories[random.nextInt(categories.length)];
    }
}
