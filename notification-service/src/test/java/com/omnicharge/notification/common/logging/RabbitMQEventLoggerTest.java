package com.omnicharge.notification.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RabbitMQEventLoggerTest {

    @Mock private LogEventPublisher logEventPublisher;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private RabbitListener rabbitListener;
    @Mock private MethodSignature signature;
    @InjectMocks private RabbitMQEventLogger logger;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(logger, "serviceName", "user-service");
        when(joinPoint.getSignature()).thenReturn(signature);
        Method m = this.getClass().getDeclaredMethod("setUp");
        when(signature.getMethod()).thenReturn(m);
        when(signature.getDeclaringType()).thenReturn(this.getClass());
    }

    // --- CONSUMPTION ---
    @Test
    void testLogConsumption_LoggingServiceReturnsImmediately() throws Throwable {
        ReflectionTestUtils.setField(logger, "serviceName", "logging-service");
        when(joinPoint.proceed()).thenReturn("done");
        
        Object result = logger.logConsumption(joinPoint, rabbitListener);
        assertEquals("done", result);
        verify(logEventPublisher, never()).publish(any());
    }

    @Test
    void testLogConsumption_LoggingQueueReturnsImmediately() throws Throwable {
        when(rabbitListener.queues()).thenReturn(new String[]{LoggingConstants.LOGGING_QUEUE});
        when(joinPoint.proceed()).thenReturn("done");
        
        Object result = logger.logConsumption(joinPoint, rabbitListener);
        assertEquals("done", result);
        verify(logEventPublisher, never()).publish(any());
    }

    @Test
    void testLogConsumption_Success() throws Throwable {
        when(rabbitListener.queues()).thenReturn(new String[]{"testQ1", "testQ2"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"StringMsg"});
        when(joinPoint.proceed()).thenReturn("done");
        
        Object result = logger.logConsumption(joinPoint, rabbitListener);
        assertEquals("done", result);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "INFO".equals(e.getLevel())));
    }

    @Test
    void testLogConsumption_NoArgs() throws Throwable {
        when(rabbitListener.queues()).thenReturn(new String[]{});
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("done");
        
        Object result = logger.logConsumption(joinPoint, rabbitListener);
        assertEquals("done", result);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "INFO".equals(e.getLevel())));
    }

    @Test
    void testLogConsumption_Exception() throws Throwable {
        when(rabbitListener.queues()).thenReturn(new String[]{"testQ"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"msg"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Consume Error"));
        
        assertThrows(RuntimeException.class, () -> logger.logConsumption(joinPoint, rabbitListener));
        verify(logEventPublisher, times(1)).publish(argThat(e -> "ERROR".equals(e.getLevel())));
    }

    // --- PUBLISHING ---
    @Test
    void testLogPublishing_LoggingServiceReturnsImmediately() throws Throwable {
        ReflectionTestUtils.setField(logger, "serviceName", "logging-service");
        when(joinPoint.proceed()).thenReturn("done");
        
        Object result = logger.logPublishing(joinPoint);
        assertEquals("done", result);
        verify(logEventPublisher, never()).publish(any());
    }

    @Test
    void testLogPublishing_LoggingExchangeReturnsImmediately() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{LoggingConstants.LOGGING_EXCHANGE, "routing", "msg"});
        when(joinPoint.proceed()).thenReturn("done");
        
        Object result = logger.logPublishing(joinPoint);
        assertEquals("done", result);
        verify(logEventPublisher, never()).publish(any());
    }

    @Test
    void testLogPublishing_ReentrantCall() throws Throwable {
        ThreadLocal<Boolean> tl = (ThreadLocal<Boolean>) ReflectionTestUtils.getField(RabbitMQEventLogger.class, "IS_LOGGING");
        tl.set(true);
        try {
            when(joinPoint.proceed()).thenReturn("done");
            Object result = logger.logPublishing(joinPoint);
            assertEquals("done", result);
            verify(logEventPublisher, never()).publish(any());
        } finally {
            tl.remove();
        }
    }

    @Test
    void testLogPublishing_Success_3Args() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"myExchange", "myRoute", "myMsg"});
        when(joinPoint.proceed()).thenReturn("done");
        
        Object result = logger.logPublishing(joinPoint);
        assertEquals("done", result);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "INFO".equals(e.getLevel())));
    }

    @Test
    void testLogPublishing_Success_2Args() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"myExchange", "myMsg"});
        when(joinPoint.proceed()).thenReturn("done");
        
        Object result = logger.logPublishing(joinPoint);
        assertEquals("done", result);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "INFO".equals(e.getLevel())));
    }
    
    @Test
    void testLogPublishing_Success_0Args() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("done");
        
        Object result = logger.logPublishing(joinPoint);
        assertEquals("done", result);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "INFO".equals(e.getLevel())));
    }

    @Test
    void testLogPublishing_Exception() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"ex", "rt", "msg"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Publish Error"));
        
        assertThrows(RuntimeException.class, () -> logger.logPublishing(joinPoint));
        verify(logEventPublisher, times(1)).publish(argThat(e -> "ERROR".equals(e.getLevel())));
    }
}
