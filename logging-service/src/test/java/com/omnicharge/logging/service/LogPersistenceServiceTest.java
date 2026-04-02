package com.omnicharge.logging.service;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.logging.entity.LogEntry;
import com.omnicharge.logging.repository.LogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogPersistenceServiceTest {

    @Mock
    private LogEntryRepository logEntryRepository;

    @InjectMocks
    private LogPersistenceService logPersistenceService;

    private LogEvent event;

    @BeforeEach
    void setUp() {
        event = LogEvent.builder()
                .serviceName("payment-service")
                .level("ERROR")
                .logger("PaymentLogger")
                .message("Payment Failed")
                .traceId("trace-123")
                .spanId("span-456")
                .threadName("main")
                .stackTrace("java.lang.Exception")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void save_PersistsSuccessfully() {
        logPersistenceService.save(event);

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logEntryRepository, times(1)).save(captor.capture());

        LogEntry savedEntry = captor.getValue();
        assertNotNull(savedEntry);
        assertEquals("payment-service", savedEntry.getServiceName());
        assertEquals("ERROR", savedEntry.getLevel());
        assertEquals("PaymentLogger", savedEntry.getLogger());
        assertEquals("Payment Failed", savedEntry.getMessage());
        assertEquals("trace-123", savedEntry.getTraceId());
        assertEquals("span-456", savedEntry.getSpanId());
        assertEquals("main", savedEntry.getThreadName());
        assertEquals("java.lang.Exception", savedEntry.getStackTrace());
        assertEquals(event.getTimestamp(), savedEntry.getTimestamp());
    }

    @Test
    void save_HandlesExceptionGracefully() {
        when(logEntryRepository.save(any(LogEntry.class))).thenThrow(new RuntimeException("DB Connection Failed"));

        // Exception should be caught inside the service
        logPersistenceService.save(event);

        verify(logEntryRepository, times(1)).save(any(LogEntry.class));
    }
}
