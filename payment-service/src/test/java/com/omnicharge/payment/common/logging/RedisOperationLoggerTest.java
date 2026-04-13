package com.omnicharge.payment.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RedisOperationLoggerTest {

    @Mock private LogEventPublisher logEventPublisher;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature signature;
    @InjectMocks private RedisOperationLogger logger;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(logger, "serviceName", "user-service");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
    }

    // --- GET ---
    @Test
    void testLogRedisGet_Hit() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"myKey"});
        when(joinPoint.proceed()).thenReturn("myValue");
        
        Object result = logger.logRedisGet(joinPoint);
        
        assertEquals("myValue", result);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "HIT".equals(e.getLevel()) || "DEBUG".equals(e.getLevel())));
    }

    @Test
    void testLogRedisGet_Miss() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"myKey"});
        when(joinPoint.proceed()).thenReturn(null);
        
        Object result = logger.logRedisGet(joinPoint);
        
        assertNull(result);
        verify(logEventPublisher, times(1)).publish(any());
    }

    @Test
    void testLogRedisGet_Exception() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"myKey"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Redis error"));
        
        assertThrows(RuntimeException.class, () -> logger.logRedisGet(joinPoint));
        verify(logEventPublisher, times(1)).publish(argThat(e -> "ERROR".equals(e.getLevel())));
    }

    @Test
    void testLogRedisGet_NoArgs() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("val");
        logger.logRedisGet(joinPoint);
        verify(logEventPublisher, times(1)).publish(any());
    }

    // --- SET ---
    @Test
    void testLogRedisSet_Success() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"myKey", "myValue"});
        when(joinPoint.proceed()).thenReturn("OK");
        
        logger.logRedisSet(joinPoint);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "DEBUG".equals(e.getLevel())));
    }

    @Test
    void testLogRedisSet_Exception() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"myKey", "myValue"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Set error"));
        
        assertThrows(RuntimeException.class, () -> logger.logRedisSet(joinPoint));
        verify(logEventPublisher, times(1)).publish(argThat(e -> "ERROR".equals(e.getLevel())));
    }

    @Test
    void testLogRedisSet_NoArgs() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("OK");
        logger.logRedisSet(joinPoint);
        verify(logEventPublisher, times(1)).publish(any());
    }

    // --- DELETE ---
    @Test
    void testLogRedisDelete_Success() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"myKey"});
        when(joinPoint.proceed()).thenReturn(Boolean.TRUE);
        
        logger.logRedisDelete(joinPoint);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "DEBUG".equals(e.getLevel())));
    }

    @Test
    void testLogRedisDelete_Exception() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"myKey"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Delete error"));
        
        assertThrows(RuntimeException.class, () -> logger.logRedisDelete(joinPoint));
        verify(logEventPublisher, times(1)).publish(argThat(e -> "ERROR".equals(e.getLevel())));
    }

    @Test
    void testLogRedisDelete_NoArgs() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn(Boolean.TRUE);
        logger.logRedisDelete(joinPoint);
        verify(logEventPublisher, times(1)).publish(any());
    }
}
