package com.omnicharge.operator.client;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.NumverifyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based test for NumverifyClient business operation logging.
 * 
 * Validates Property 33: Business Operation Event Logging
 * "For any external API call (Numverify), the system should log the event with
 * relevant business context including phone number, response time, and status."
 * 
 * This test verifies that Numverify API calls publish log events
 * with appropriate business context.
 */
@ExtendWith(MockitoExtension.class)
@Tag("Feature: production-grade-centralized-logging, Property 33: Business Operation Event Logging")
class NumverifyClientBusinessOperationPropertyTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private NumverifyClient numverifyClient;

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
    }

    @Test
    void property_numverifyApiCallSuccess_shouldLogWithBusinessContext() {
        // Property: Successful Numverify API calls must log with phone number, response time, and status
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            String phoneNumber = generateRandomPhoneNumber();
            NumverifyResponse response = createSuccessResponse();
            
            when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
                    .thenReturn(response);
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            NumverifyResponse result = numverifyClient.detectOperator(phoneNumber);
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("NUMVERIFY_API_CALL");
            assertThat(capturedEvent.getMessage()).contains("Numverify API call successful");
            assertThat(capturedEvent.getMessage()).contains("mobile=" + phoneNumber);
            
            // Verify business context
            Map<String, Object> context = capturedEvent.getContext();
            assertThat(context).containsKey("mobileNumber");
            assertThat(context).containsKey("responseTimeMs");
            assertThat(context).containsKey("responseStatus");
            assertThat(context).containsEntry("responseStatus", "SUCCESS");
            assertThat(context.get("mobileNumber")).isEqualTo(phoneNumber);
            
            // Verify response time is a reasonable number
            Long responseTime = (Long) context.get("responseTimeMs");
            assertThat(responseTime).isNotNull();
            assertThat(responseTime).isGreaterThanOrEqualTo(0L);
            
            // Reset mocks
            reset(logEventPublisher, restTemplate);
        }
    }

    @Test
    void property_numverifyApiCallFailure_shouldLogWithErrorContext() {
        // Property: Failed Numverify API calls must log with phone number, error message, and status
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            String phoneNumber = generateRandomPhoneNumber();
            
            when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
                    .thenThrow(new RuntimeException("API connection failed"));
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act & Assert
            try {
                numverifyClient.detectOperator(phoneNumber);
            } catch (Exception e) {
                // Expected exception
            }
            
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("NUMVERIFY_API_CALL");
            assertThat(capturedEvent.getMessage()).contains("Numverify API call failed");
            assertThat(capturedEvent.getMessage()).contains("mobile=" + phoneNumber);
            
            // Verify business context
            Map<String, Object> context = capturedEvent.getContext();
            assertThat(context).containsKey("mobileNumber");
            assertThat(context).containsKey("responseStatus");
            assertThat(context).containsEntry("responseStatus", "FAILED");
            assertThat(context).containsKey("errorMessage");
            assertThat(context.get("mobileNumber")).isEqualTo(phoneNumber);
            
            // Reset mocks
            reset(logEventPublisher, restTemplate);
        }
    }

    @Test
    void property_allNumverifyLogs_shouldContainPhoneNumber() {
        // Property: All Numverify API logs must contain phone number
        // Run 100+ iterations
        
        for (int i = 0; i < 100; i++) {
            String phoneNumber = generateRandomPhoneNumber();
            NumverifyResponse response = createSuccessResponse();
            
            when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
                    .thenReturn(response);
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            numverifyClient.detectOperator(phoneNumber);
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent event = logEventCaptor.getValue();
            Map<String, Object> context = event.getContext();
            assertThat(context).containsKey("mobileNumber");
            assertThat(context.get("mobileNumber")).isEqualTo(phoneNumber);
            assertThat(event.getServiceName()).isEqualTo("operator-service");
            assertThat(event.getTimestamp()).isNotNull();
            
            // Reset mocks
            reset(logEventPublisher, restTemplate);
        }
    }

    @Test
    void property_allNumverifyLogs_shouldContainResponseTime() {
        // Property: All Numverify API logs must contain response time
        // Run 100+ iterations with mixed success/failure scenarios
        
        for (int i = 0; i < 100; i++) {
            String phoneNumber = generateRandomPhoneNumber();
            boolean shouldSucceed = random.nextBoolean();
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            if (shouldSucceed) {
                NumverifyResponse response = createSuccessResponse();
                when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
                        .thenReturn(response);
                
                numverifyClient.detectOperator(phoneNumber);
            } else {
                when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
                        .thenThrow(new RuntimeException("API Error"));
                
                try {
                    numverifyClient.detectOperator(phoneNumber);
                } catch (Exception e) {
                    // Expected
                }
            }
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent event = logEventCaptor.getValue();
            Map<String, Object> context = event.getContext();
            assertThat(context).containsKey("responseTimeMs");
            
            Long responseTime = (Long) context.get("responseTimeMs");
            assertThat(responseTime).isNotNull();
            assertThat(responseTime).isGreaterThanOrEqualTo(0L);
            
            // Reset mocks
            reset(logEventPublisher, restTemplate);
        }
    }

    @Test
    void property_allNumverifyLogs_shouldContainStatus() {
        // Property: All Numverify API logs must contain status (SUCCESS/FAILURE)
        // Run 100+ iterations with mixed success/failure scenarios
        
        for (int i = 0; i < 100; i++) {
            String phoneNumber = generateRandomPhoneNumber();
            boolean shouldSucceed = random.nextBoolean();
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            if (shouldSucceed) {
                NumverifyResponse response = createSuccessResponse();
                when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
                        .thenReturn(response);
                
                numverifyClient.detectOperator(phoneNumber);
            } else {
                when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
                        .thenThrow(new RuntimeException("API Error"));
                
                try {
                    numverifyClient.detectOperator(phoneNumber);
                } catch (Exception e) {
                    // Expected
                }
            }
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent event = logEventCaptor.getValue();
            Map<String, Object> context = event.getContext();
            assertThat(context).containsKey("responseStatus");
            
            String status = (String) context.get("responseStatus");
            assertThat(status).isIn("SUCCESS", "FAILED");
            
            // Reset mocks
            reset(logEventPublisher, restTemplate);
        }
    }

    // Helper methods to generate random test data
    
    private String generateRandomPhoneNumber() {
        // Generate Indian phone numbers (10 digits starting with 6-9)
        int firstDigit = 6 + random.nextInt(4); // 6, 7, 8, or 9
        long remainingDigits = random.nextLong(100000000, 999999999);
        return String.valueOf(firstDigit) + remainingDigits;
    }
    
    private NumverifyResponse createSuccessResponse() {
        NumverifyResponse response = new NumverifyResponse();
        response.setValid(true);
        response.setCarrier("Test Carrier " + random.nextInt(100));
        response.setCountryCode("IN");
        return response;
    }
}
