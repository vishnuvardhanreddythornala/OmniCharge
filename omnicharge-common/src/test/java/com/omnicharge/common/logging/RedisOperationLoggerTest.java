package com.omnicharge.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisOperationLogger AOP aspect.
 * Validates Requirements 4.1, 4.2, 4.3: Redis operations are logged with hit/miss/error status.
 */
@ExtendWith(MockitoExtension.class)
class RedisOperationLoggerTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private RedisOperationLogger redisOperationLogger;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(redisOperationLogger, "serviceName", "test-service");
    }

    @Test
    void logRedisGet_shouldLogCacheHit() throws Throwable {
        // Arrange
        String key = "user:123";
        when(joinPoint.getArgs()).thenReturn(new Object[]{key});
        when(joinPoint.proceed()).thenReturn("cached-value");

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act
        Object result = redisOperationLogger.logRedisGet(joinPoint);

        // Assert
        assertThat(result).isEqualTo("cached-value");
        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        verify(joinPoint, times(1)).proceed();
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getServiceName()).isEqualTo("test-service");
        assertThat(capturedEvent.getLevel()).isEqualTo("DEBUG");
        assertThat(capturedEvent.getEventType()).isEqualTo("REDIS");
        assertThat(capturedEvent.getMessage()).contains("[REDIS-GET]");
        assertThat(capturedEvent.getMessage()).contains(key);
        assertThat(capturedEvent.getMessage()).contains("HIT");
        assertThat(capturedEvent.getTimestamp()).isNotNull();
    }

    @Test
    void logRedisGet_shouldLogCacheMiss() throws Throwable {
        // Arrange
        String key = "user:999";
        when(joinPoint.getArgs()).thenReturn(new Object[]{key});
        when(joinPoint.proceed()).thenReturn(null); // Cache miss

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act
        Object result = redisOperationLogger.logRedisGet(joinPoint);

        // Assert
        assertThat(result).isNull();
        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getLevel()).isEqualTo("DEBUG");
        assertThat(capturedEvent.getEventType()).isEqualTo("REDIS");
        assertThat(capturedEvent.getMessage()).contains("MISS");
    }

    @Test
    void logRedisGet_shouldLogConnectionError() throws Throwable {
        // Arrange
        String key = "user:123";
        when(joinPoint.getArgs()).thenReturn(new Object[]{key});
        
        RuntimeException exception = new RuntimeException("Connection timeout");
        when(joinPoint.proceed()).thenThrow(exception);

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act & Assert
        assertThatThrownBy(() -> redisOperationLogger.logRedisGet(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Connection timeout");

        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getLevel()).isEqualTo("ERROR");
        assertThat(capturedEvent.getEventType()).isEqualTo("REDIS");
        assertThat(capturedEvent.getMessage()).contains("ERROR");
        assertThat(capturedEvent.getMessage()).contains("Connection timeout");
        assertThat(capturedEvent.getStackTrace()).isEqualTo("Connection timeout");
    }

    @Test
    void logRedisSet_shouldLogSuccessfulCacheWrite() throws Throwable {
        // Arrange
        String key = "user:123";
        when(joinPoint.getArgs()).thenReturn(new Object[]{key, "value"});
        when(joinPoint.proceed()).thenReturn(null);

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act
        redisOperationLogger.logRedisSet(joinPoint);

        // Assert
        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        verify(joinPoint, times(1)).proceed();
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getServiceName()).isEqualTo("test-service");
        assertThat(capturedEvent.getLevel()).isEqualTo("DEBUG");
        assertThat(capturedEvent.getEventType()).isEqualTo("REDIS");
        assertThat(capturedEvent.getMessage()).contains("[REDIS-SET]");
        assertThat(capturedEvent.getMessage()).contains(key);
        assertThat(capturedEvent.getMessage()).contains("SUCCESS");
    }

    @Test
    void logRedisSet_shouldLogFailedCacheWrite() throws Throwable {
        // Arrange
        String key = "user:123";
        when(joinPoint.getArgs()).thenReturn(new Object[]{key, "value"});
        
        RuntimeException exception = new RuntimeException("Write failed");
        when(joinPoint.proceed()).thenThrow(exception);

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act & Assert
        assertThatThrownBy(() -> redisOperationLogger.logRedisSet(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Write failed");

        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getLevel()).isEqualTo("ERROR");
        assertThat(capturedEvent.getEventType()).isEqualTo("REDIS");
        assertThat(capturedEvent.getMessage()).contains("FAILED");
        assertThat(capturedEvent.getMessage()).contains("Write failed");
    }

    @Test
    void logRedisDelete_shouldLogSuccessfulCacheEviction() throws Throwable {
        // Arrange
        String key = "user:123";
        when(joinPoint.getArgs()).thenReturn(new Object[]{key});
        when(joinPoint.proceed()).thenReturn(true);

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act
        redisOperationLogger.logRedisDelete(joinPoint);

        // Assert
        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        verify(joinPoint, times(1)).proceed();
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getServiceName()).isEqualTo("test-service");
        assertThat(capturedEvent.getLevel()).isEqualTo("DEBUG");
        assertThat(capturedEvent.getEventType()).isEqualTo("REDIS");
        assertThat(capturedEvent.getMessage()).contains("[REDIS-DELETE]");
        assertThat(capturedEvent.getMessage()).contains(key);
        assertThat(capturedEvent.getMessage()).contains("SUCCESS");
    }

    @Test
    void logRedisDelete_shouldLogFailedCacheEviction() throws Throwable {
        // Arrange
        String key = "user:123";
        when(joinPoint.getArgs()).thenReturn(new Object[]{key});
        
        RuntimeException exception = new RuntimeException("Delete failed");
        when(joinPoint.proceed()).thenThrow(exception);

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act & Assert
        assertThatThrownBy(() -> redisOperationLogger.logRedisDelete(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Delete failed");

        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getLevel()).isEqualTo("ERROR");
        assertThat(capturedEvent.getEventType()).isEqualTo("REDIS");
        assertThat(capturedEvent.getMessage()).contains("FAILED");
        assertThat(capturedEvent.getMessage()).contains("Delete failed");
    }
}
