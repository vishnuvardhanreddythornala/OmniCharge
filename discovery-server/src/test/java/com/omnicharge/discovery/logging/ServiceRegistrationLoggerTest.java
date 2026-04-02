package com.omnicharge.discovery.logging;

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
 * Unit tests for ServiceRegistrationLogger.
 * 
 * Validates that service registration events are logged correctly
 * with proper context and event structure.
 */
@ExtendWith(MockitoExtension.class)
class ServiceRegistrationLoggerTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @Captor
    private ArgumentCaptor<LogEvent> logEventCaptor;

    private ServiceRegistrationLogger serviceRegistrationLogger;

    @BeforeEach
    void setUp() {
        serviceRegistrationLogger = new ServiceRegistrationLogger(logEventPublisher);
    }

    @Test
    void logServiceRegistration_WithValidParameters_LogsCorrectly() {
        // Arrange
        String serviceName = "user-service";
        String instanceId = "user-service-instance-1";
        String status = "UP";

        // Act
        serviceRegistrationLogger.logServiceRegistration(serviceName, instanceId, status);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("discovery-server", logEvent.getServiceName());
        assertEquals("INFO", logEvent.getLevel());
        assertEquals("SERVICE_REGISTRATION", logEvent.getEventType());
        assertTrue(logEvent.getMessage().contains("user-service"));
        assertTrue(logEvent.getMessage().contains("user-service-instance-1"));
        assertTrue(logEvent.getMessage().contains("UP"));
        
        assertNotNull(logEvent.getContext());
        assertEquals("user-service", logEvent.getContext().get("serviceName"));
        assertEquals("user-service-instance-1", logEvent.getContext().get("instanceId"));
        assertEquals("UP", logEvent.getContext().get("status"));
        assertEquals("REGISTRATION", logEvent.getContext().get("eventType"));
    }

    @Test
    void logServiceFailure_WithValidParameters_LogsCorrectly() {
        // Arrange
        String serviceName = "payment-service";
        String instanceId = "payment-service-instance-2";
        String reason = "Connection timeout";

        // Act
        serviceRegistrationLogger.logServiceFailure(serviceName, instanceId, reason);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("discovery-server", logEvent.getServiceName());
        assertEquals("WARN", logEvent.getLevel());
        assertEquals("SERVICE_FAILURE", logEvent.getEventType());
        assertTrue(logEvent.getMessage().contains("payment-service"));
        assertTrue(logEvent.getMessage().contains("payment-service-instance-2"));
        assertTrue(logEvent.getMessage().contains("Connection timeout"));
        
        assertNotNull(logEvent.getContext());
        assertEquals("payment-service", logEvent.getContext().get("serviceName"));
        assertEquals("payment-service-instance-2", logEvent.getContext().get("instanceId"));
        assertEquals("Connection timeout", logEvent.getContext().get("reason"));
        assertEquals("FAILURE", logEvent.getContext().get("eventType"));
    }

    @Test
    void logHeartbeatFailure_WithValidParameters_LogsCorrectly() {
        // Arrange
        String serviceName = "recharge-service";
        String instanceId = "recharge-service-instance-3";

        // Act
        serviceRegistrationLogger.logHeartbeatFailure(serviceName, instanceId);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("discovery-server", logEvent.getServiceName());
        assertEquals("WARN", logEvent.getLevel());
        assertEquals("HEARTBEAT_FAILURE", logEvent.getEventType());
        assertTrue(logEvent.getMessage().contains("recharge-service"));
        assertTrue(logEvent.getMessage().contains("recharge-service-instance-3"));
        assertTrue(logEvent.getMessage().contains("Heartbeat missed"));
        
        assertNotNull(logEvent.getContext());
        assertEquals("recharge-service", logEvent.getContext().get("serviceName"));
        assertEquals("recharge-service-instance-3", logEvent.getContext().get("instanceId"));
        assertEquals("HEARTBEAT_FAILURE", logEvent.getContext().get("eventType"));
    }
}
