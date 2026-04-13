package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
import com.omnicharge.operator.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class PlanQueryServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ObjectMapper objectMapper;
    @Mock private PlanRepository planRepository;
    @Mock private OperatorEventPublisher operatorEventPublisher;

    @InjectMocks
    private PlanQueryService planQueryService;

    private PlanResponse planResponse;
    private PlanResponse planResponse2;
    private Plan plan;
    private Operator operator;

    @BeforeEach
    void setUp() {
        planResponse = PlanResponse.builder()
                .id(1L)
                .planName("Basic")
                .price(new BigDecimal("99"))
                .validityDays(28)
                .category(PlanCategory.RECOMMENDED)
                .build();

        planResponse2 = PlanResponse.builder()
                .id(2L)
                .planName("Premium")
                .price(new BigDecimal("299"))
                .validityDays(56)
                .category(PlanCategory.DATA)
                .build();

        operator = new Operator();
        operator.setId(10L);
        operator.setName("TestOp");

        plan = new Plan();
        plan.setId(1L);
        plan.setOperator(operator);
        plan.setPlanName("Basic");
        plan.setPrice(new BigDecimal("99"));
        plan.setValidityDays(28);
        plan.setDataLimit("1GB");
        plan.setCallBenefit("Unlimited");
        plan.setSmsBenefit("100");
        plan.setAdditionalBenefits("Hotstar");
        plan.setCategory(PlanCategory.RECOMMENDED);
        plan.setIsActive(true);
    }

    // ===== getPlanById =====

    @Test
    void getPlanById_CacheHit() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plan:detail:1")).thenReturn("json");
        when(objectMapper.readValue("json", PlanResponse.class)).thenReturn(planResponse);

        PlanResponse result = planQueryService.getPlanById(1L);

        assertNotNull(result);
        assertEquals("Basic", result.getPlanName());
    }

    @Test
    void getPlanById_CacheMiss_FallbackToDb() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plan:detail:1")).thenReturn(null);
        when(planRepository.findActiveById(1L)).thenReturn(Optional.of(plan));

        PlanResponse result = planQueryService.getPlanById(1L);

        assertNotNull(result);
        assertEquals("Basic", result.getPlanName());
        verify(operatorEventPublisher).publishPlanUpdatedEvent(10L);
    }

    @Test
    void getPlanById_RedisError_Throws() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis Down"));

        assertThrows(RuntimeException.class, () -> planQueryService.getPlanById(1L));
    }

    @Test
    void getPlanById_JsonParseError_Throws() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plan:detail:1")).thenReturn("bad-json");
        when(objectMapper.readValue("bad-json", PlanResponse.class)).thenThrow(new JsonProcessingException("parse") {});

        assertThrows(RuntimeException.class, () -> planQueryService.getPlanById(1L));
    }

    // ===== fallbackGetPlanById =====

    @Test
    void fallbackGetPlanById_Success() {
        when(planRepository.findActiveById(1L)).thenReturn(Optional.of(plan));

        PlanResponse result = planQueryService.fallbackGetPlanById(1L, new RuntimeException("test fallback"));

        assertNotNull(result);
        assertEquals("Basic", result.getPlanName());
        verify(operatorEventPublisher).publishPlanUpdatedEvent(10L);
    }

    @Test
    void fallbackGetPlanById_NotFound() {
        when(planRepository.findActiveById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> planQueryService.fallbackGetPlanById(999L, new RuntimeException("err")));
    }

    // ===== searchPlansFromRedis =====

    @Test
    void searchPlansFromRedis_CacheHit_NoFilters() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:10")).thenReturn("[]");
        when(objectMapper.readValue(eq("[]"), any(TypeReference.class)))
                .thenReturn(List.of(planResponse, planResponse2));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(10L, null, null, null, pageable);

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void searchPlansFromRedis_CacheHit_FilterByCategory() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:10")).thenReturn("[]");
        when(objectMapper.readValue(eq("[]"), any(TypeReference.class)))
                .thenReturn(List.of(planResponse, planResponse2));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(10L, PlanCategory.RECOMMENDED, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Basic", result.getContent().get(0).getPlanName());
    }

    @Test
    void searchPlansFromRedis_CacheHit_FilterByMinPrice() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:10")).thenReturn("[]");
        when(objectMapper.readValue(eq("[]"), any(TypeReference.class)))
                .thenReturn(List.of(planResponse, planResponse2));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(
                10L, null, new BigDecimal("200"), null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Premium", result.getContent().get(0).getPlanName());
    }

    @Test
    void searchPlansFromRedis_CacheHit_FilterByMaxPrice() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:10")).thenReturn("[]");
        when(objectMapper.readValue(eq("[]"), any(TypeReference.class)))
                .thenReturn(List.of(planResponse, planResponse2));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(
                10L, null, null, new BigDecimal("150"), pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchPlansFromRedis_CacheHit_SortByValidityDaysDesc() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:10")).thenReturn("[]");
        when(objectMapper.readValue(eq("[]"), any(TypeReference.class)))
                .thenReturn(List.of(planResponse, planResponse2));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("validityDays").descending());
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(10L, null, null, null, pageable);

        assertEquals(2, result.getTotalElements());
        // First should be Premium (56 days) since descending
        assertEquals("Premium", result.getContent().get(0).getPlanName());
    }

    @Test
    void searchPlansFromRedis_CacheHit_PaginationBeyondResults() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:10")).thenReturn("[]");
        when(objectMapper.readValue(eq("[]"), any(TypeReference.class)))
                .thenReturn(List.of(planResponse));

        Pageable pageable = PageRequest.of(5, 10, Sort.by("price").ascending());
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(10L, null, null, null, pageable);

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void searchPlansFromRedis_CacheMiss_FallbackToDb() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:10")).thenReturn(null);

        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        Page<Plan> dbPage = new PageImpl<>(List.of(plan));
        when(planRepository.searchActivePlans(eq(10L), isNull(), isNull(), isNull(), any()))
                .thenReturn(dbPage);

        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(10L, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(operatorEventPublisher).publishPlanUpdatedEvent(10L);
    }

    @Test
    void searchPlansFromRedis_RedisError_Throws() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis Down"));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        assertThrows(RuntimeException.class,
                () -> planQueryService.searchPlansFromRedis(10L, null, null, null, pageable));
    }

    // ===== fallbackSearchPlans =====

    @Test
    void fallbackSearchPlans_Success() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        Page<Plan> dbPage = new PageImpl<>(List.of(plan));
        when(planRepository.searchActivePlans(eq(10L), isNull(), isNull(), isNull(), any()))
                .thenReturn(dbPage);

        Page<PlanResponse> result = planQueryService.fallbackSearchPlans(
                10L, null, null, null, pageable, new RuntimeException("test"));

        assertEquals(1, result.getTotalElements());
        verify(operatorEventPublisher).publishPlanUpdatedEvent(10L);
    }
}
