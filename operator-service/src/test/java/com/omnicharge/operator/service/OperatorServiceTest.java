package com.omnicharge.operator.service;

import com.omnicharge.operator.common.exception.DuplicateResourceException;
import com.omnicharge.operator.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.common.logging.LogEvent;
import com.omnicharge.operator.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.OperatorRequest;
import com.omnicharge.operator.dto.OperatorResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.OperatorCategory;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
import com.omnicharge.operator.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperatorServiceTest {

    @Mock private OperatorRepository operatorRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private LogEventPublisher logEventPublisher;
    @Mock private OperatorEventPublisher operatorEventPublisher;

    @InjectMocks
    private OperatorService operatorService;

    private Operator operator;
    private Plan plan;

    @BeforeEach
    void setUp() {
        operator = new Operator();
        operator.setId(1L);
        operator.setName("Jio");
        operator.setCode("JIO");
        operator.setCategory(OperatorCategory.PREPAID);
        operator.setIsActive(true);
        operator.setPlans(new ArrayList<>());
        
        plan = new Plan();
        plan.setId(10L);
        plan.setIsActive(true);
        operator.getPlans().add(plan);
    }

    @Test
    void getOperatorById_Success() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        OperatorResponse response = operatorService.getOperatorById(1L);
        assertNotNull(response);
        assertEquals("Jio", response.getName());
    }

    @Test
    void getOperatorById_NotFound() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> operatorService.getOperatorById(1L));
    }

    @Test
    void getActiveOperatorById_Success() {
        when(operatorRepository.findActiveById(1L)).thenReturn(Optional.of(operator));
        OperatorResponse response = operatorService.getActiveOperatorById(1L);
        assertTrue(response.getIsActive());
    }

    @Test
    void getOperatorsByCategory_Success() {
        when(operatorRepository.findByCategory(OperatorCategory.PREPAID)).thenReturn(List.of(operator));
        List<OperatorResponse> list = operatorService.getOperatorsByCategory(OperatorCategory.PREPAID);
        assertEquals(1, list.size());
    }

    @Test
    void getAllOperators_Success() {
        when(operatorRepository.findAll()).thenReturn(List.of(operator));
        assertEquals(1, operatorService.getAllOperators().size());
    }

    @Test
    void createOperator_Success() {
        OperatorRequest req = new OperatorRequest();
        req.setName("Airtel");
        req.setCode("AIR");
        req.setCategory(OperatorCategory.PREPAID);

        when(operatorRepository.existsByCode("AIR")).thenReturn(false);
        when(operatorRepository.existsByName("Airtel")).thenReturn(false);
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);

        OperatorResponse res = operatorService.createOperator(req);
        assertNotNull(res);
        verify(operatorRepository, times(1)).save(any(Operator.class));
        verify(redisTemplate, times(1)).delete("operators:active");
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void createOperator_DuplicateCode() {
        OperatorRequest req = new OperatorRequest();
        req.setCode("JIO");
        when(operatorRepository.existsByCode("JIO")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> operatorService.createOperator(req));
    }

    @Test
    void updateOperator_Success() {
        OperatorRequest req = new OperatorRequest();
        req.setName("Jio New");
        req.setCode("JIONew");
        req.setCategory(OperatorCategory.PREPAID);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.findByCode("JIONew")).thenReturn(Optional.empty());
        when(operatorRepository.findByName("Jio New")).thenReturn(Optional.empty());
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);

        operatorService.updateOperator(1L, req);

        verify(operatorRepository, times(1)).save(operator);
        verify(redisTemplate, times(1)).delete("operators:active");
    }

    @Test
    void updateOperator_DuplicateName() {
        Operator other = new Operator();
        other.setId(2L);
        OperatorRequest req = new OperatorRequest();
        req.setName("Existing");
        
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.findByCode(req.getCode())).thenReturn(Optional.empty());
        when(operatorRepository.findByName("Existing")).thenReturn(Optional.of(other));

        assertThrows(DuplicateResourceException.class, () -> operatorService.updateOperator(1L, req));
    }

    @Test
    void deleteOperator_CascadesToPlans() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        operatorService.deleteOperator(1L);

        assertFalse(operator.getIsActive());
        assertFalse(plan.getIsActive());
        assertTrue(plan.getDeactivatedByOperator());
        verify(operatorRepository, times(1)).save(operator);
        verify(redisTemplate, times(1)).delete("operators:active");
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void activateOperator_RestoresDeactivatedPlans() {
        operator.setIsActive(false);
        plan.setIsActive(false);
        plan.setDeactivatedByOperator(true);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(operator)).thenReturn(operator);

        operatorService.activateOperator(1L);

        assertTrue(operator.getIsActive());
        assertTrue(plan.getIsActive());
        assertFalse(plan.getDeactivatedByOperator());
        verify(operatorRepository, times(1)).save(operator);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }
    
    @Test
    void deactivateOperator_DeactivatesAllActivePlans() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(operator)).thenReturn(operator);

        operatorService.deactivateOperator(1L);

        assertFalse(operator.getIsActive());
        assertFalse(plan.getIsActive());
        assertTrue(plan.getDeactivatedByOperator());
        verify(operatorRepository, times(1)).save(operator);
    }
}
