package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.client.NumverifyClient;
import com.omnicharge.operator.dto.NumverifyResponse;
import com.omnicharge.operator.dto.OperatorDetectionResponse;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperatorDetectionService implements IOperatorDetectionService {

    private final NumverifyClient numverifyClient;
    private final OperatorRepository operatorRepository;
    private final IPlanService planService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LogEventPublisher logEventPublisher;

    private static final long CACHE_TTL_HOURS = 24;

    @Override
    public OperatorDetectionResponse detectOperator(String mobileNumber) {
        // Check Redis cache first
        String cacheKey = "operator:detect:" + mobileNumber;
        String cachedResponse = null;
        try {
            cachedResponse = redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis is down, skipping cache for mobile: {}", mobileNumber);
        }
        
        if (cachedResponse != null) {
            try {
                log.info("Cache hit for mobile: {}", mobileNumber);
                return objectMapper.readValue(cachedResponse, OperatorDetectionResponse.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cached response", e);
            }
        }

        // Call Numverify API
        NumverifyResponse numverifyResponse = numverifyClient.detectOperator(mobileNumber);
        
        Operator operator = null;
        
        if (numverifyResponse != null && numverifyResponse.getValid() && numverifyResponse.getCarrier() != null) {
            // Try to match carrier name to operator
            operator = matchCarrierToOperator(numverifyResponse.getCarrier());
        }
        
        // Fallback to prefix-based detection if Numverify fails
        if (operator == null) {
            log.info("Numverify failed or no match, using prefix-based detection");
            operator = detectByPrefix(mobileNumber);
        }

        if (operator == null) {
            log.warn("Could not detect operator for mobile: {}", mobileNumber);
            
            // Log failed detection
            Map<String, Object> context = new HashMap<>();
            context.put("mobileNumber", mobileNumber);
            context.put("detectionResult", "FAILED");
            context.put("reason", "No operator match found");
            publishBusinessLog("OPERATOR_DETECTION",
                "Operator detection failed: mobile=" + mobileNumber,
                context);
            
            return null;
        }

        // Get active plans for the operator
        List<PlanResponse> plans = planService.getPlansByOperator(operator.getId());

        // Build response
        OperatorDetectionResponse response = OperatorDetectionResponse.builder()
                .operatorId(operator.getId())
                .operatorName(operator.getName())
                .operatorCode(operator.getCode())
                .logoUrl(operator.getLogoUrl())
                .plans(plans)
                .build();

        // Log successful detection
        Map<String, Object> context = new HashMap<>();
        context.put("mobileNumber", mobileNumber);
        context.put("detectionResult", "SUCCESS");
        context.put("operatorId", operator.getId());
        context.put("operatorName", operator.getName());
        context.put("operatorCode", operator.getCode());
        context.put("plansCount", plans.size());
        publishBusinessLog("OPERATOR_DETECTION",
            "Operator detected: mobile=" + mobileNumber + ", operator=" + operator.getName(),
            context);

        // Cache the response
        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            try {
                redisTemplate.opsForValue().set(cacheKey, jsonResponse, CACHE_TTL_HOURS, TimeUnit.HOURS);
                log.info("Cached operator detection for mobile: {}", mobileNumber);
            } catch (Exception e) {
                log.warn("Redis is down, unable to cache data for mobile: {}", mobileNumber);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for caching", e);
        }

        return response;
    }

    private Operator matchCarrierToOperator(String carrier) {
        // Fuzzy matching logic
        String carrierLower = carrier.toLowerCase();
        
        List<Operator> allOperators = operatorRepository.findByIsActive(true);
        
        for (Operator operator : allOperators) {
            String operatorNameLower = operator.getName().toLowerCase();
            String operatorCodeLower = operator.getCode().toLowerCase();
            
            // Check if carrier contains operator name or code
            if (carrierLower.contains(operatorNameLower) || 
                carrierLower.contains(operatorCodeLower) ||
                operatorNameLower.contains(carrierLower)) {
                log.info("Matched carrier '{}' to operator '{}'", carrier, operator.getName());
                return operator;
            }
        }
        
        // Specific mappings for common variations
        if (carrierLower.contains("bharti") || carrierLower.contains("airtel")) {
            return operatorRepository.findByCode("AIRTEL").orElse(null);
        } else if (carrierLower.contains("jio") || carrierLower.contains("reliance")) {
            return operatorRepository.findByCode("JIO").orElse(null);
        } else if (carrierLower.contains("vodafone") || carrierLower.contains("idea") || carrierLower.contains("vi")) {
            return operatorRepository.findByCode("VI").orElse(null);
        } else if (carrierLower.contains("bsnl")) {
            return operatorRepository.findByCode("BSNL").orElse(null);
        }
        
        return null;
    }

    private Operator detectByPrefix(String mobileNumber) {
        // Simple prefix-based detection (first 4 digits)
        if (mobileNumber.length() < 4) {
            return null;
        }
        
        String prefix = mobileNumber.substring(0, 4);
        
        // Common prefixes (this is a simplified version)
        // In production, you'd have a comprehensive prefix-to-operator mapping table
        if (prefix.matches("(9876|9988|9910|9811)")) {
            return operatorRepository.findByCode("AIRTEL").orElse(null);
        } else if (prefix.matches("(9999|8888|7777)")) {
            return operatorRepository.findByCode("JIO").orElse(null);
        } else if (prefix.matches("(9898|9090|8080)")) {
            return operatorRepository.findByCode("VI").orElse(null);
        }
        
        // Default to first active operator if no match
        List<Operator> activeOperators = operatorRepository.findByIsActive(true);
        return activeOperators.isEmpty() ? null : activeOperators.get(0);
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
