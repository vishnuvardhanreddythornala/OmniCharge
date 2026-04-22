package com.omnicharge.gateway.common.logging;


import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RedisOperationLoggerTest {

    @Mock private LogEventPublisher logEventPublisher;
    @Mock private ProceedingJoinPoint joinPoint;
    @InjectMocks private RedisOperationLogger logger;

    @BeforeEach
    void setUp()  {
        ReflectionTestUtils.setField(logger, "serviceName", "api-gateway");
    }

    // --- GET ---
    @Test
    void testLogRedisGet_Hit() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"user:123"});
        when(joinPoint.proceed()).thenReturn("cachedValue");
        Object result = logger.logRedisGet(joinPoint);
        assertEquals("cachedValue", result);
        verify(logEventPublisher).publish(argThat(e -> e.getMessage().contains("HIT")));
    }

    @Test
    void testLogRedisGet_Miss() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"user:404"});
        when(joinPoint.proceed()).thenReturn(null);
        Object result = logger.logRedisGet(joinPoint);
        assertNull(result);
        verify(logEventPublisher).publish(argThat(e -> e.getMessage().contains("MISS")));
    }

    @Test
    void testLogRedisGet_Error() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"user:err"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Redis down"));
        assertThrows(RuntimeException.class, () -> logger.logRedisGet(joinPoint));
        verify(logEventPublisher).publish(argThat(e -> "ERROR".equals(e.getLevel())));
    }

    @Test
    void testLogRedisGet_NoArgs() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("val");
        logger.logRedisGet(joinPoint);
        verify(logEventPublisher).publish(argThat(e -> e.getMessage().contains("unknown")));
    }

    // --- SET ---
    @Test
    void testLogRedisSet_Success() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1", "value1"});
        when(joinPoint.proceed()).thenReturn(null);
        logger.logRedisSet(joinPoint);
        verify(logEventPublisher).publish(argThat(e -> e.getMessage().contains("SUCCESS")));
    }

    @Test
    void testLogRedisSet_Error() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1", "value1"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Redis down"));
        assertThrows(RuntimeException.class, () -> logger.logRedisSet(joinPoint));
        verify(logEventPublisher).publish(argThat(e -> "ERROR".equals(e.getLevel())));
    }

    @Test
    void testLogRedisSet_NoArgs() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn(null);
        logger.logRedisSet(joinPoint);
        verify(logEventPublisher).publish(argThat(e -> e.getMessage().contains("unknown")));
    }

    // --- DELETE ---
    @Test
    void testLogRedisDelete_Success() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        when(joinPoint.proceed()).thenReturn(true);
        Object result = logger.logRedisDelete(joinPoint);
        assertEquals(true, result);
        verify(logEventPublisher).publish(argThat(e -> e.getMessage().contains("SUCCESS")));
    }

    @Test
    void testLogRedisDelete_Error() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Redis down"));
        assertThrows(RuntimeException.class, () -> logger.logRedisDelete(joinPoint));
        verify(logEventPublisher).publish(argThat(e -> "ERROR".equals(e.getLevel())));
    }

    @Test
    void testLogRedisDelete_NoArgs() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn(true);
        logger.logRedisDelete(joinPoint);
        verify(logEventPublisher).publish(argThat(e -> e.getMessage().contains("unknown")));
    }
}
