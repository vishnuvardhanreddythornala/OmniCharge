package com.omnicharge.config.logging;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfigRequestLogger.
 * 
 * Validates that configuration requests are logged correctly
 * with proper context and event structure.
 */
@ExtendWith(MockitoExtension.class)
class ConfigRequestLoggerTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @Captor
    private ArgumentCaptor<LogEvent> logEventCaptor;

    private ConfigRequestLogger configRequestLogger;

    @BeforeEach
    void setUp() {
        configRequestLogger = new ConfigRequestLogger(logEventPublisher);
    }

    @Test
    void logConfigRequest_WithAllParameters_LogsCorrectly() {
        // Arrange
        String application = "user-service";
        String profile = "prod";
        String label = "main";

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("config-server", logEvent.getServiceName());
        assertEquals("INFO", logEvent.getLevel());
        assertEquals("CONFIG_REQUEST", logEvent.getEventType());
        assertTrue(logEvent.getMessage().contains("user-service"));
        assertTrue(logEvent.getMessage().contains("prod"));
        assertTrue(logEvent.getMessage().contains("main"));
        
        assertNotNull(logEvent.getContext());
        assertEquals("user-service", logEvent.getContext().get("application"));
        assertEquals("prod", logEvent.getContext().get("profile"));
        assertEquals("main", logEvent.getContext().get("label"));
    }

    @Test
    void logConfigRequest_WithNullProfile_UsesDefault() {
        // Arrange
        String application = "api-gateway";
        String profile = null;
        String label = "master";

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("default", logEvent.getContext().get("profile"));
        assertTrue(logEvent.getMessage().contains("default"));
    }

    @Test
    void logConfigRequest_WithNullLabel_UsesMaster() {
        // Arrange
        String application = "payment-service";
        String profile = "dev";
        String label = null;

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("master", logEvent.getContext().get("label"));
        assertTrue(logEvent.getMessage().contains("master"));
    }
}
