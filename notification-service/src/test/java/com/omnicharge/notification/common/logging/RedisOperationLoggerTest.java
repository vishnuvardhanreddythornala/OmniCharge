package com.omnicharge.notification.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RedisOperationLoggerTest {
    @Mock private LogEventPublisher logEventPublisher;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature signature;
    @InjectMocks private RedisOperationLogger logger;

    @Test
    void testLogRedisGet() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("test");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key"});
        when(joinPoint.proceed()).thenReturn(null);
        logger.logRedisGet(joinPoint);
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void testLogRedisSet() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("test");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key", "val"});
        when(joinPoint.proceed()).thenReturn(null);
        logger.logRedisSet(joinPoint);
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }
    
    @Test
    void testLogRedisDelete() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("test");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key"});
        when(joinPoint.proceed()).thenReturn(null);
        logger.logRedisDelete(joinPoint);
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }
}
