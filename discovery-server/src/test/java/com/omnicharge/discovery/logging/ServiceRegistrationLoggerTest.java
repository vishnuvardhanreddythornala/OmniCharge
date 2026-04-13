package com.omnicharge.discovery.logging;

import com.omnicharge.discovery.common.logging.LogEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRegistrationLoggerTest {

    @Mock private LogEventPublisher logEventPublisher;
    @InjectMocks private ServiceRegistrationLogger logger;

    @Test
    void logServiceRegistration_Success() {
        logger.logServiceRegistration("user-service", "user-service:8001", "UP");
        verify(logEventPublisher).publish(argThat(e ->
                e.getMessage().contains("SERVICE-REGISTRATION") &&
                e.getMessage().contains("user-service") &&
                "INFO".equals(e.getLevel())
        ));
    }

    @Test
    void logServiceRegistration_ExceptionHandledGracefully() {
        doThrow(new RuntimeException("Publish fail")).when(logEventPublisher).publish(any());
        assertDoesNotThrow(() -> logger.logServiceRegistration("svc", "id", "UP"));
    }

    @Test
    void logServiceFailure_Success() {
        logger.logServiceFailure("payment-service", "payment:8003", "Instance cancelled");
        verify(logEventPublisher).publish(argThat(e ->
                e.getMessage().contains("SERVICE-FAILURE") &&
                "WARN".equals(e.getLevel())
        ));
    }

    @Test
    void logServiceFailure_ExceptionHandledGracefully() {
        doThrow(new RuntimeException("Publish fail")).when(logEventPublisher).publish(any());
        assertDoesNotThrow(() -> logger.logServiceFailure("svc", "id", "reason"));
    }

    @Test
    void logHeartbeatFailure_Success() {
        logger.logHeartbeatFailure("recharge-service", "recharge:8004");
        verify(logEventPublisher).publish(argThat(e ->
                e.getMessage().contains("HEARTBEAT-FAILURE") &&
                "WARN".equals(e.getLevel())
        ));
    }

    @Test
    void logHeartbeatFailure_ExceptionHandledGracefully() {
        doThrow(new RuntimeException("Publish fail")).when(logEventPublisher).publish(any());
        assertDoesNotThrow(() -> logger.logHeartbeatFailure("svc", "id"));
    }
}
