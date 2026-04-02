package com.omnicharge.operator.service;

import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.OperatorRequest;
import com.omnicharge.operator.dto.OperatorResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
import com.omnicharge.operator.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Property-based test for OperatorService business operation logging.
 * 
 * Validates Property 33: Business Operation Event Logging
 * "For any critical business operation (operator creation, update, activation, deactivation),
 * the system should log the event with relevant business context including entity IDs,
 * operator codes, and statuses."
 * 
 * This test verifies that all business operations in OperatorService publish
 * log events with appropriate business context.
 */
@ExtendWith(MockitoExtension.class)
@Tag("Feature: production-grade-centralized-logging, Property 33: Business Operation Event Logging")
class OperatorServiceBusinessOperationPropertyTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorEventPublisher operatorEventPublisher;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private OperatorService operatorService;

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
    }

    @Test
    void property_operatorCreation_shouldLogWithBusinessContext() {
        // Property: Operator creation must log with operatorId, code, name, and status
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            OperatorRequest request = createRandomOperatorRequest();
            Operator savedOperator = createOperatorFromRequest(request);
            
            when(operatorRepository.existsByCode(anyString())).thenReturn(false);
            when(operatorRepository.save(any(Operator.class))).thenReturn(savedOperator);
            doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(anyLong());
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            OperatorResponse result = operatorService.createOperator(request);
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("OPERATOR_CREATED");
            assertThat(capturedEvent.getMessage()).contains("Operator created");
            assertThat(capturedEvent.getMessage()).contains("name=" + savedOperator.getName());
            
            // Verify business context
            assertThat(capturedEvent.getContext()).isNotNull();
            Map<String, Object> context = capturedEvent.getContext();
            assertThat(context).containsKey("operatorId");
            assertThat(context).containsKey("operatorCode");
            assertThat(context).containsKey("operatorName");
            assertThat(context).containsKey("category");
            assertThat(context.get("operatorId")).isEqualTo(savedOperator.getId());
            assertThat(context.get("operatorCode")).isEqualTo(savedOperator.getCode());
            assertThat(context.get("operatorName")).isEqualTo(savedOperator.getName());
            
            // Reset mocks
            reset(logEventPublisher, operatorRepository, operatorEventPublisher);
        }
    }

    @Test
    void property_operatorUpdate_shouldLogWithBusinessContext() {
        // Property: Operator update must log with operatorId, code, name, and updated fields
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            Long operatorId = random.nextLong(1, 10000);
            OperatorRequest request = createRandomOperatorRequest();
            Operator existingOperator = createRandomOperator();
            existingOperator.setId(operatorId);
            
            when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(existingOperator));
            when(operatorRepository.save(any(Operator.class))).thenReturn(existingOperator);
            doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(anyLong());
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            OperatorResponse result = operatorService.updateOperator(operatorId, request);
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("OPERATOR_UPDATED");
            assertThat(capturedEvent.getMessage()).contains("Operator updated");
            assertThat(capturedEvent.getMessage()).contains("name=");
            
            // Verify business context
            Map<String, Object> context = capturedEvent.getContext();
            assertThat(context).containsKey("operatorId");
            assertThat(context).containsKey("operatorCode");
            assertThat(context).containsKey("operatorName");
            assertThat(context).containsKey("category");
            assertThat(context.get("operatorId")).isEqualTo(operatorId);
            
            // Reset mocks
            reset(logEventPublisher, operatorRepository, operatorEventPublisher);
        }
    }

    @Test
    void property_operatorActivation_shouldLogWithBusinessContext() {
        // Property: Operator activation must log with operatorId, code, and new status
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            Long operatorId = random.nextLong(1, 10000);
            Operator operator = createRandomOperator();
            operator.setId(operatorId);
            operator.setIsActive(false); // Initially inactive
            
            when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator));
            when(operatorRepository.save(any(Operator.class))).thenReturn(operator);
            doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(anyLong());
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            operatorService.activateOperator(operatorId);
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("OPERATOR_ACTIVATED");
            assertThat(capturedEvent.getMessage()).contains("Operator activated");
            assertThat(capturedEvent.getMessage()).contains("name=");
            
            // Verify business context
            Map<String, Object> context = capturedEvent.getContext();
            assertThat(context).containsKey("operatorId");
            assertThat(context).containsKey("operatorCode");
            assertThat(context).containsKey("operatorName");
            assertThat(context).containsKey("restoredPlansCount");
            assertThat(context.get("operatorId")).isEqualTo(operatorId);
            
            // Reset mocks
            reset(logEventPublisher, operatorRepository, operatorEventPublisher);
        }
    }

    @Test
    void property_operatorDeactivation_shouldLogWithBusinessContext() {
        // Property: Operator deactivation must log with operatorId, code, and new status
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            Long operatorId = random.nextLong(1, 10000);
            Operator operator = createRandomOperator();
            operator.setId(operatorId);
            operator.setIsActive(true); // Initially active
            
            when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator));
            when(operatorRepository.save(any(Operator.class))).thenReturn(operator);
            doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(anyLong());
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            operatorService.deactivateOperator(operatorId);
            
            // Assert
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent capturedEvent = logEventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("OPERATOR_DEACTIVATED");
            assertThat(capturedEvent.getMessage()).contains("Operator deactivated");
            assertThat(capturedEvent.getMessage()).contains("name=");
            
            // Verify business context
            Map<String, Object> context = capturedEvent.getContext();
            assertThat(context).containsKey("operatorId");
            assertThat(context).containsKey("operatorCode");
            assertThat(context).containsKey("operatorName");
            assertThat(context).containsKey("deactivatedPlansCount");
            assertThat(context).containsKey("reason");
            assertThat(context.get("operatorId")).isEqualTo(operatorId);
            
            // Reset mocks
            reset(logEventPublisher, operatorRepository, operatorEventPublisher);
        }
    }

    @Test
    void property_allBusinessLogs_shouldContainTimestamp() {
        // Property: All business logs must contain timestamp
        // Run 100+ iterations
        
        for (int i = 0; i < 100; i++) {
            OperatorRequest request = createRandomOperatorRequest();
            Operator savedOperator = createOperatorFromRequest(request);
            
            when(operatorRepository.existsByCode(anyString())).thenReturn(false);
            when(operatorRepository.save(any(Operator.class))).thenReturn(savedOperator);
            doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(anyLong());
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            operatorService.createOperator(request);
            
            // Assert: All business logs contain timestamp
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent event = logEventCaptor.getValue();
            assertThat(event.getTimestamp()).isNotNull();
            assertThat(event.getServiceName()).isEqualTo("operator-service");
            
            // Reset mocks
            reset(logEventPublisher, operatorRepository, operatorEventPublisher);
        }
    }

    // Helper methods to generate random test data
    
    private OperatorRequest createRandomOperatorRequest() {
        OperatorRequest request = new OperatorRequest();
        request.setName("Operator " + random.nextInt(1000));
        request.setCode("OP" + random.nextInt(1000));
        request.setCategory(getRandomOperatorCategory());
        request.setLogoUrl("https://example.com/logo" + random.nextInt(1000) + ".png");
        return request;
    }
    
    private Operator createRandomOperator() {
        Operator operator = new Operator();
        operator.setId(random.nextLong(1, 10000));
        operator.setName("Operator " + random.nextInt(1000));
        operator.setCode("OP" + random.nextInt(1000));
        operator.setCategory(getRandomOperatorCategory());
        operator.setLogoUrl("https://example.com/logo.png");
        operator.setIsActive(random.nextBoolean());
        return operator;
    }
    
    private Operator createOperatorFromRequest(OperatorRequest request) {
        Operator operator = new Operator();
        operator.setId(random.nextLong(1, 10000));
        operator.setName(request.getName());
        operator.setCode(request.getCode());
        operator.setCategory(request.getCategory());
        operator.setLogoUrl(request.getLogoUrl());
        operator.setIsActive(true);
        return operator;
    }
    
    private com.omnicharge.operator.entity.OperatorCategory getRandomOperatorCategory() {
        com.omnicharge.operator.entity.OperatorCategory[] categories = com.omnicharge.operator.entity.OperatorCategory.values();
        return categories[random.nextInt(categories.length)];
    }
}
