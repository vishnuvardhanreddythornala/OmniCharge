package com.omnicharge.discovery.common.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ServiceLifecycleLoggerTest {
    @Mock private LogEventPublisher logEventPublisher;
    @InjectMocks private ServiceLifecycleLogger logger;

    @Test
    void testOnReady() {
        logger.onApplicationReady(mock(ApplicationReadyEvent.class));
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void testOnShutdown() {
        logger.onApplicationShutdown(null);
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }
}
