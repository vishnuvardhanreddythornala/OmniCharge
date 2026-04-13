package com.omnicharge.logging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.logging.common.logging.LogEvent;
import com.omnicharge.logging.entity.LogEntry;
import com.omnicharge.logging.repository.LogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogPersistenceServiceTest {

    @Mock private LogEntryRepository logEntryRepository;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private com.omnicharge.logging.common.logging.LogEventPublisher logEventPublisher;


    @InjectMocks
    private LogPersistenceService logPersistenceService;

    private LogEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent = new LogEvent();
        validEvent.setServiceName("user-service");
        validEvent.setLevel("INFO");
        validEvent.setLogger("com.omnicharge.user.service.AuthService");
        validEvent.setMessage("OTP sent successfully");
        validEvent.setTraceId("trace-001");
        validEvent.setSpanId("span-001");
        validEvent.setThreadName("main");
        validEvent.setEventType("OTP_SENT");
        validEvent.setTimestamp(LocalDateTime.now());
        validEvent.setContext(Map.of("userId", "1", "mobile", "9876543210"));
    }

    @Test
    @DisplayName("SUCCESS: Saves log entry with context JSON")
    void save_Success_WithContext() {
        logPersistenceService.save(validEvent);

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logEntryRepository, times(1)).save(captor.capture());

        LogEntry saved = captor.getValue();
        assertEquals("user-service", saved.getServiceName());
        assertEquals("INFO", saved.getLevel());
        assertEquals("OTP_SENT", saved.getEventType());
        assertNotNull(saved.getContextJson());
        assertTrue(saved.getContextJson().contains("userId"));
    }

    @Test
    @DisplayName("SUCCESS: Saves log entry without context (null)")
    void save_Success_NullContext() {
        validEvent.setContext(null);

        logPersistenceService.save(validEvent);

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logEntryRepository, times(1)).save(captor.capture());
        assertNull(captor.getValue().getContextJson());
    }

    @Test
    @DisplayName("SUCCESS: Saves log entry with empty context")
    void save_Success_EmptyContext() {
        validEvent.setContext(Map.of());

        logPersistenceService.save(validEvent);

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logEntryRepository, times(1)).save(captor.capture());
        assertNull(captor.getValue().getContextJson());
    }

    @Test
    @DisplayName("FAIL: DB error is caught gracefully (no exception propagated)")
    void save_DbFailure_NoException() {
        when(logEntryRepository.save(any())).thenThrow(new RuntimeException("DB connection failed"));

        // Should NOT throw
        assertDoesNotThrow(() -> logPersistenceService.save(validEvent));
    }
}
