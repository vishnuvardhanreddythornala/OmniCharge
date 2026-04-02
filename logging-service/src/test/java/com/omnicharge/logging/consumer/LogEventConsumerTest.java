package com.omnicharge.logging.consumer;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.logging.service.LogFileWriterService;
import com.omnicharge.logging.service.LogPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogEventConsumerTest {

    @Mock
    private LogPersistenceService logPersistenceService;

    @Mock
    private LogFileWriterService logFileWriterService;

    @InjectMocks
    private LogEventConsumer logEventConsumer;

    private LogEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = LogEvent.builder()
                .serviceName("user-service")
                .level("INFO")
                .logger("TestLogger")
                .message("Test Message")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void consumeLogEvent_SuccessfullyCallsBothServices() {
        logEventConsumer.consumeLogEvent(sampleEvent);

        verify(logFileWriterService, times(1)).writeToFile(sampleEvent);
        verify(logPersistenceService, times(1)).save(sampleEvent);
    }

    @Test
    void consumeLogEvent_IsolatesFailures() {
        doThrow(new RuntimeException("DB Error")).when(logPersistenceService).save(any(LogEvent.class));

        // Should handle exception and not break the application
        logEventConsumer.consumeLogEvent(sampleEvent);

        verify(logFileWriterService, times(1)).writeToFile(sampleEvent);
        verify(logPersistenceService, times(1)).save(sampleEvent);
    }
}
