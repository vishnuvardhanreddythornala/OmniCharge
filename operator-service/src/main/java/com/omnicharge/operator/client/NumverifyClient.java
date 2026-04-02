package com.omnicharge.operator.client;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.NumverifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NumverifyClient {

    @Value("${numverify.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final LogEventPublisher logEventPublisher;

    public NumverifyResponse detectOperator(String mobileNumber) {
        String url = String.format(
            "http://apilayer.net/api/validate?access_key=%s&number=91%s",
            apiKey,
            mobileNumber
        );
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Calling Numverify API for mobile: {}", mobileNumber);
            
            NumverifyResponse response = restTemplate.getForObject(url, NumverifyResponse.class);
            
            long responseTime = System.currentTimeMillis() - startTime;
            log.info("Numverify response: {}", response);
            
            // Log successful API call
            Map<String, Object> context = new HashMap<>();
            context.put("apiEndpoint", "http://apilayer.net/api/validate");
            context.put("mobileNumber", mobileNumber);
            context.put("responseStatus", "SUCCESS");
            context.put("responseTimeMs", responseTime);
            context.put("carrierDetected", response != null && response.getCarrier() != null ? response.getCarrier() : "null");
            publishBusinessLog("NUMVERIFY_API_CALL",
                "Numverify API call successful: mobile=" + mobileNumber + ", responseTime=" + responseTime + "ms",
                context);
            
            return response;
        } catch (Exception e) {
            log.error("Failed to call Numverify API for mobile: {}", mobileNumber, e);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Log failed API call
            Map<String, Object> context = new HashMap<>();
            context.put("apiEndpoint", "http://apilayer.net/api/validate");
            context.put("mobileNumber", mobileNumber);
            context.put("responseStatus", "FAILED");
            context.put("responseTimeMs", responseTime);
            context.put("errorMessage", e.getMessage());
            publishBusinessLog("NUMVERIFY_API_CALL",
                "Numverify API call failed: mobile=" + mobileNumber + ", error=" + e.getMessage(),
                context);
            
            return null;
        }
    }
    
    // Helper method for business operation logging
    private void publishBusinessLog(String eventType, String message, Map<String, Object> context) {
        LogEvent logEvent = LogEvent.builder()
                .serviceName("operator-service")
                .level("INFO")
                .logger(this.getClass().getName())
                .message(message)
                .eventType(eventType)
                .context(context)
                .timestamp(LocalDateTime.now())
                .build();
        logEventPublisher.publish(logEvent);
    }
}
