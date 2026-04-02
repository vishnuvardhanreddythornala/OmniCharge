package com.omnicharge.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RabbitMQEventLogger AOP aspect.
 * Validates Requirements 3.1, 3.2, 3.3: RabbitMQ message publishing and consumption are logged.
 */
@ExtendWith(MockitoExtension.class)
class RabbitMQEventLoggerTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private RabbitMQEventLogger rabbitMQEventLogger;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rabbitMQEventLogger, "serviceName", "test-service");
    }

    @Test
    void logConsumption_shouldLogSuccessfulMessageConsumption() throws Throwable {
        // Arrange
        RabbitListener rabbitListener = mock(RabbitListener.class);
        when(rabbitListener.queues()).thenReturn(new String[]{"test.queue"});
        
        Method method = TestConsumer.class.getMethod("handleMessage", TestEvent.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getDeclaringType()).thenReturn((Class) TestConsumer.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{new TestEvent()});
        when(joinPoint.proceed()).thenReturn(null);

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act
        rabbitMQEventLogger.logConsumption(joinPoint, rabbitListener);

        // Assert
        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        verify(joinPoint, times(1)).proceed();
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getServiceName()).isEqualTo("test-service");
        assertThat(capturedEvent.getLevel()).isEqualTo("INFO");
        assertThat(capturedEvent.getEventType()).isEqualTo("RABBITMQ");
        assertThat(capturedEvent.getMessage()).contains("[RABBITMQ-CONSUME]");
        assertThat(capturedEvent.getMessage()).contains("test.queue");
        assertThat(capturedEvent.getMessage()).contains("TestEvent");
        assertThat(capturedEvent.getMessage()).contains("SUCCESS");
        assertThat(capturedEvent.getTimestamp()).isNotNull();
    }

    @Test
    void logConsumption_shouldLogFailedMessageConsumption() throws Throwable {
        // Arrange
        RabbitListener rabbitListener = mock(RabbitListener.class);
        when(rabbitListener.queues()).thenReturn(new String[]{"test.queue"});
        
        Method method = TestConsumer.class.getMethod("handleMessage", TestEvent.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getDeclaringType()).thenReturn((Class) TestConsumer.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{new TestEvent()});
        
        RuntimeException exception = new RuntimeException("Processing failed");
        when(joinPoint.proceed()).thenThrow(exception);

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act & Assert
        assertThatThrownBy(() -> rabbitMQEventLogger.logConsumption(joinPoint, rabbitListener))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Processing failed");

        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getLevel()).isEqualTo("ERROR");
        assertThat(capturedEvent.getEventType()).isEqualTo("RABBITMQ");
        assertThat(capturedEvent.getMessage()).contains("FAILED");
        assertThat(capturedEvent.getMessage()).contains("Processing failed");
        assertThat(capturedEvent.getStackTrace()).isEqualTo("Processing failed");
    }

    @Test
    void logPublishing_shouldLogSuccessfulMessagePublishing() throws Throwable {
        // Arrange
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test.exchange", "test.routing.key", new TestEvent()});
        when(joinPoint.proceed()).thenReturn(null);

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act
        rabbitMQEventLogger.logPublishing(joinPoint);

        // Assert
        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        verify(joinPoint, times(1)).proceed();
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getServiceName()).isEqualTo("test-service");
        assertThat(capturedEvent.getLevel()).isEqualTo("INFO");
        assertThat(capturedEvent.getEventType()).isEqualTo("RABBITMQ");
        assertThat(capturedEvent.getMessage()).contains("[RABBITMQ-PUBLISH]");
        assertThat(capturedEvent.getMessage()).contains("test.exchange");
        assertThat(capturedEvent.getMessage()).contains("test.routing.key");
        assertThat(capturedEvent.getMessage()).contains("TestEvent");
        assertThat(capturedEvent.getMessage()).contains("SUCCESS");
    }

    @Test
    void logPublishing_shouldLogFailedMessagePublishing() throws Throwable {
        // Arrange
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test.exchange", "test.routing.key", new TestEvent()});
        
        RuntimeException exception = new RuntimeException("Connection failed");
        when(joinPoint.proceed()).thenThrow(exception);

        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act & Assert
        assertThatThrownBy(() -> rabbitMQEventLogger.logPublishing(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Connection failed");

        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getLevel()).isEqualTo("ERROR");
        assertThat(capturedEvent.getEventType()).isEqualTo("RABBITMQ");
        assertThat(capturedEvent.getMessage()).contains("FAILED");
        assertThat(capturedEvent.getMessage()).contains("Connection failed");
    }

    // Test helper classes
    static class TestConsumer {
        public void handleMessage(TestEvent event) {
            // Test consumer method
        }
    }

    static class TestEvent {
        // Test event class
    }
}
