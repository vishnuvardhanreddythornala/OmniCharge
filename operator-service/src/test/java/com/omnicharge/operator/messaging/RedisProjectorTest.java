package com.omnicharge.operator.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.event.PlanUpdatedMessage;
import com.omnicharge.operator.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisProjectorTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private com.omnicharge.operator.common.logging.LogEventPublisher logEventPublisher;


    @InjectMocks
    private RedisProjector redisProjector;

    private PlanUpdatedMessage message;
    private Plan plan;

    @BeforeEach
    void setUp() {
        message = PlanUpdatedMessage.builder()
                .eventId("test-uuid-123")
                .operatorId(1L)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        Operator operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");

        plan = new Plan();
        plan.setId(10L);
        plan.setOperator(operator);
        plan.setPlanName("Unlimited");
        plan.setPrice(new BigDecimal("199.00"));
        plan.setCategory(PlanCategory.UNLIMITED);
        plan.setIsActive(true);
    }

    @Test
    void consumePlanUpdatedEvent_Success_NewEvent() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("event:processed:test-uuid-123"), eq("true"), any(Duration.class)))
                .thenReturn(true);
        when(planRepository.findByOperatorIdAndIsActive(1L, true)).thenReturn(List.of(plan));
        when(objectMapper.writeValueAsString(any())).thenReturn("[{json}]");

        redisProjector.consumePlanUpdatedEvent(message);

        verify(planRepository, times(1)).findByOperatorIdAndIsActive(1L, true);
        verify(valueOperations, times(1)).set(eq("plans:operator:1"), anyString());
        verify(valueOperations, times(1)).set(eq("plan:detail:10"), anyString());
    }

    @Test
    void consumePlanUpdatedEvent_DuplicateEvent_IsIgnored() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(false);

        redisProjector.consumePlanUpdatedEvent(message);

        verify(planRepository, never()).findByOperatorIdAndIsActive(anyLong(), anyBoolean());
        verify(valueOperations, never()).set(eq("plans:operator:1"), anyString());
    }
}
