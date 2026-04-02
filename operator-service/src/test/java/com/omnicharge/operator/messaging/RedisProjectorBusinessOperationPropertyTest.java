package com.omnicharge.operator.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.event.PlanUpdatedMessage;
import com.omnicharge.operator.repository.PlanRepository;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for RedisProjector business operation logging.
 * 
 * Validates Property 33: Business Operation Event Logging
 * "For any RabbitMQ event consumption, the system should log the event with
 * relevant business context including eventId, operatorId, and processing outcome."
 * 
 * This test verifies that RabbitMQ event consumption publishes log events
 * with appropriate business context.
 */
@ExtendWith(MockitoExtension.class)
@Tag("Feature: production-grade-centralized-logging, Property 33: Business Operation Event Logging")
class RedisProjectorBusinessOperationPropertyTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private RedisProjector redisProjector;

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random();
    }

    @Test
    void property_rabbitmqEventReceived_shouldLogWithBusinessContext() throws Exception {
        // Property: RabbitMQ event received must log with eventId, operatorId, and eventType
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            PlanUpdatedMessage message = createRandomPlanUpdatedMessage();
            Plan plan = createRandomPlan(message.getOperatorId());
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .thenReturn(true); // New event
            when(planRepository.findByOperatorIdAndIsActive(anyLong(), anyBoolean()))
                    .thenReturn(List.of(plan));
            lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            redisProjector.consumePlanUpdatedEvent(message);
            
            // Assert - Find the RABBITMQ_RECEIVE event with eventType=EVENT_RECEIVED
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent receivedEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "RABBITMQ_RECEIVE".equals(e.getEventType()))
                    .filter(e -> e.getContext() != null && "RECEIVED".equals(e.getContext().get("processingStatus")))
                    .findFirst()
                    .orElse(null);
            
            assertThat(receivedEvent).isNotNull();
            assertThat(receivedEvent.getMessage()).contains("Plan update event received");
            assertThat(receivedEvent.getMessage()).contains("eventId=" + message.getEventId());
            
            // Verify business context
            Map<String, Object> context = receivedEvent.getContext();
            assertThat(context).containsKey("eventId");
            assertThat(context).containsKey("operatorId");
            assertThat(context).containsKey("processingStatus");
            assertThat(context).containsEntry("processingStatus", "RECEIVED");
            assertThat(context.get("eventId")).isEqualTo(message.getEventId());
            assertThat(context.get("operatorId")).isEqualTo(message.getOperatorId());
            
            // Reset mocks
            reset(logEventPublisher, planRepository, redisTemplate);
        }
    }

    @Test
    void property_rabbitmqEventDuplicate_shouldLogWithBusinessContext() {
        // Property: Duplicate RabbitMQ events must log with eventId and duplicate status
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            PlanUpdatedMessage message = createRandomPlanUpdatedMessage();
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .thenReturn(false); // Duplicate event
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            redisProjector.consumePlanUpdatedEvent(message);
            
            // Assert - Find the RABBITMQ_RECEIVE event with eventType=DUPLICATE_IGNORED
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent duplicateEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "RABBITMQ_RECEIVE".equals(e.getEventType()))
                    .filter(e -> e.getContext() != null && "DUPLICATE_IGNORED".equals(e.getContext().get("processingStatus")))
                    .findFirst()
                    .orElse(null);
            
            assertThat(duplicateEvent).isNotNull();
            assertThat(duplicateEvent.getMessage()).contains("Duplicate plan update event ignored");
            assertThat(duplicateEvent.getMessage()).contains("eventId=" + message.getEventId());
            
            // Verify business context
            Map<String, Object> context = duplicateEvent.getContext();
            assertThat(context).containsKey("eventId");
            assertThat(context).containsKey("operatorId");
            assertThat(context).containsKey("processingStatus");
            assertThat(context).containsEntry("processingStatus", "DUPLICATE_IGNORED");
            assertThat(context.get("eventId")).isEqualTo(message.getEventId());
            
            // Reset mocks
            reset(logEventPublisher, redisTemplate);
        }
    }

    @Test
    void property_rabbitmqEventSuccess_shouldLogWithBusinessContext() throws Exception {
        // Property: Successful RabbitMQ event processing must log with eventId, operatorId, and plan count
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            PlanUpdatedMessage message = createRandomPlanUpdatedMessage();
            int planCount = random.nextInt(10) + 1;
            List<Plan> plans = createRandomPlans(message.getOperatorId(), planCount);
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .thenReturn(true);
            when(planRepository.findByOperatorIdAndIsActive(anyLong(), anyBoolean()))
                    .thenReturn(plans);
            lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            redisProjector.consumePlanUpdatedEvent(message);
            
            // Assert - Find the RABBITMQ_RECEIVE event with eventType=SUCCESS
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent successEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "RABBITMQ_RECEIVE".equals(e.getEventType()))
                    .filter(e -> e.getContext() != null && "SUCCESS".equals(e.getContext().get("processingStatus")))
                    .findFirst()
                    .orElse(null);
            
            assertThat(successEvent).isNotNull();
            assertThat(successEvent.getMessage()).contains("Plan update event processed successfully");
            assertThat(successEvent.getMessage()).contains("eventId=" + message.getEventId());
            
            // Verify business context
            Map<String, Object> context = successEvent.getContext();
            assertThat(context).containsKey("eventId");
            assertThat(context).containsKey("operatorId");
            assertThat(context).containsKey("processingStatus");
            assertThat(context).containsKey("plansUpdated");
            assertThat(context).containsEntry("processingStatus", "SUCCESS");
            assertThat(context.get("plansUpdated")).isEqualTo(planCount);
            
            // Reset mocks
            reset(logEventPublisher, planRepository, redisTemplate);
        }
    }

    @Test
    void property_rabbitmqEventFailure_shouldLogWithErrorContext() {
        // Property: Failed RabbitMQ event processing must log with eventId, operatorId, and error message
        // Run 100+ iterations with randomized inputs
        
        for (int i = 0; i < 100; i++) {
            // Arrange
            PlanUpdatedMessage message = createRandomPlanUpdatedMessage();
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .thenReturn(true);
            when(planRepository.findByOperatorIdAndIsActive(anyLong(), anyBoolean()))
                    .thenThrow(new RuntimeException("Database error"));
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            // Act
            redisProjector.consumePlanUpdatedEvent(message);
            
            // Assert - Find the RABBITMQ_RECEIVE event with eventType=FAILURE
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            LogEvent failureEvent = logEventCaptor.getAllValues().stream()
                    .filter(e -> "RABBITMQ_RECEIVE".equals(e.getEventType()))
                    .filter(e -> e.getContext() != null && "FAILED".equals(e.getContext().get("processingStatus")))
                    .findFirst()
                    .orElse(null);
            
            assertThat(failureEvent).isNotNull();
            assertThat(failureEvent.getMessage()).contains("Plan update event processing failed");
            assertThat(failureEvent.getMessage()).contains("eventId=" + message.getEventId());
            
            // Verify business context
            Map<String, Object> context = failureEvent.getContext();
            assertThat(context).containsKey("eventId");
            assertThat(context).containsKey("operatorId");
            assertThat(context).containsKey("processingStatus");
            assertThat(context).containsKey("errorMessage");
            assertThat(context).containsEntry("processingStatus", "FAILED");
            
            // Reset mocks
            reset(logEventPublisher, planRepository, redisTemplate);
        }
    }

    @Test
    void property_allRabbitmqLogs_shouldContainEventId() throws Exception {
        // Property: All RabbitMQ event logs must contain eventId
        // Run 100+ iterations with mixed scenarios
        
        for (int i = 0; i < 100; i++) {
            PlanUpdatedMessage message = createRandomPlanUpdatedMessage();
            
            // Randomly choose scenario: new event, duplicate, or failure
            int scenario = random.nextInt(3);
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            
            ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);
            
            if (scenario == 0) {
                // New event success
                when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                        .thenReturn(true);
                when(planRepository.findByOperatorIdAndIsActive(anyLong(), anyBoolean()))
                        .thenReturn(createRandomPlans(message.getOperatorId(), 1));
                lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            } else if (scenario == 1) {
                // Duplicate event
                when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                        .thenReturn(false);
            } else {
                // Failure
                when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                        .thenReturn(true);
                when(planRepository.findByOperatorIdAndIsActive(anyLong(), anyBoolean()))
                        .thenThrow(new RuntimeException("Error"));
            }
            
            // Act
            redisProjector.consumePlanUpdatedEvent(message);
            
            // Assert: All RABBITMQ_RECEIVE events contain eventId
            verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
            
            for (LogEvent event : logEventCaptor.getAllValues()) {
                if ("RABBITMQ_RECEIVE".equals(event.getEventType())) {
                    Map<String, Object> context = event.getContext();
                    assertThat(context).containsKey("eventId");
                    assertThat(context.get("eventId")).isEqualTo(message.getEventId());
                    assertThat(event.getServiceName()).isEqualTo("operator-service");
                    assertThat(event.getTimestamp()).isNotNull();
                }
            }
            
            // Reset mocks
            reset(logEventPublisher, planRepository, redisTemplate);
        }
    }

    // Helper methods to generate random test data
    
    private PlanUpdatedMessage createRandomPlanUpdatedMessage() {
        return PlanUpdatedMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .operatorId(random.nextLong(1, 100))
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
    
    private Plan createRandomPlan(Long operatorId) {
        Operator operator = new Operator();
        operator.setId(operatorId);
        operator.setName("Operator " + operatorId);
        operator.setCode("OP" + operatorId);
        operator.setIsActive(true);
        
        Plan plan = new Plan();
        plan.setId(random.nextLong(1, 10000));
        plan.setOperator(operator);
        plan.setPlanName("Plan " + random.nextInt(1000));
        plan.setPrice(new BigDecimal(random.nextInt(500) + 100));
        plan.setCategory(getRandomCategory());
        plan.setValidityDays(random.nextInt(90) + 1);
        plan.setIsActive(true);
        return plan;
    }
    
    private List<Plan> createRandomPlans(Long operatorId, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createRandomPlan(operatorId))
                .toList();
    }
    
    private PlanCategory getRandomCategory() {
        PlanCategory[] categories = PlanCategory.values();
        return categories[random.nextInt(categories.length)];
    }
}
