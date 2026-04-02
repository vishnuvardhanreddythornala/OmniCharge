package com.omnicharge.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ServiceLifecycleLogger.
 * Validates Requirements 2.1, 2.2: Service lifecycle events are logged automatically.
 */
@ExtendWith(MockitoExtension.class)
class ServiceLifecycleLoggerTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private ServiceLifecycleLogger serviceLifecycleLogger;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(serviceLifecycleLogger, "serviceName", "test-service");
    }

    @Test
    void onApplicationReady_shouldPublishStartingLifecycleEvent() {
        // Arrange
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act
        serviceLifecycleLogger.onApplicationReady(event);

        // Assert
        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getServiceName()).isEqualTo("test-service");
        assertThat(capturedEvent.getLevel()).isEqualTo("LIFECYCLE");
        assertThat(capturedEvent.getEventType()).isEqualTo("LIFECYCLE");
        assertThat(capturedEvent.getMessage()).contains("test-service STARTED successfully");
        assertThat(capturedEvent.getLogger()).isEqualTo(ServiceLifecycleLogger.class.getName());
        assertThat(capturedEvent.getTimestamp()).isNotNull();
        assertThat(capturedEvent.getThreadName()).isNotNull();
    }

    @Test
    void onApplicationShutdown_shouldPublishEndingLifecycleEvent() {
        // Arrange
        ContextClosedEvent event = mock(ContextClosedEvent.class);
        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act
        serviceLifecycleLogger.onApplicationShutdown(event);

        // Assert
        verify(logEventPublisher, times(1)).publish(logEventCaptor.capture());
        
        LogEvent capturedEvent = logEventCaptor.getValue();
        assertThat(capturedEvent.getServiceName()).isEqualTo("test-service");
        assertThat(capturedEvent.getLevel()).isEqualTo("LIFECYCLE");
        assertThat(capturedEvent.getEventType()).isEqualTo("LIFECYCLE");
        assertThat(capturedEvent.getMessage()).contains("test-service STOPPING gracefully");
        assertThat(capturedEvent.getLogger()).isEqualTo(ServiceLifecycleLogger.class.getName());
        assertThat(capturedEvent.getTimestamp()).isNotNull();
        assertThat(capturedEvent.getThreadName()).isNotNull();
    }

    @Test
    void lifecycleEvents_shouldHaveCorrectEventTypeForFiltering() {
        // This test validates that lifecycle events can be filtered for all-services.log
        // Requirement 6.3: LIFECYCLE events should be routed to all-services.log
        
        ApplicationReadyEvent startEvent = mock(ApplicationReadyEvent.class);
        ContextClosedEvent stopEvent = mock(ContextClosedEvent.class);
        ArgumentCaptor<LogEvent> logEventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        // Act
        serviceLifecycleLogger.onApplicationReady(startEvent);
        serviceLifecycleLogger.onApplicationShutdown(stopEvent);

        // Assert
        verify(logEventPublisher, times(2)).publish(logEventCaptor.capture());
        
        for (LogEvent event : logEventCaptor.getAllValues()) {
            assertThat(event.getEventType()).isEqualTo("LIFECYCLE");
            assertThat(event.getLevel()).isEqualTo("LIFECYCLE");
        }
    }
}
