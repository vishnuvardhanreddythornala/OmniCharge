package com.omnicharge.gateway.common.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceLifecycleLoggerTest {

    @Mock private LogEventPublisher logEventPublisher;
    @Mock private ApplicationReadyEvent readyEvent;
    @Mock private ContextClosedEvent closedEvent;
    @InjectMocks private ServiceLifecycleLogger logger;

    @Test
    void testOnApplicationReady() {
        logger.onApplicationReady(readyEvent);
        verify(logEventPublisher).publish(argThat(e -> e.getMessage().contains("STARTED")));
    }

    @Test
    void testOnApplicationShutdown() {
        logger.onApplicationShutdown(closedEvent);
        verify(logEventPublisher).publish(argThat(e -> e.getMessage().contains("STOPPING")));
    }
}
