package com.omnicharge.logging.consumer;

import com.omnicharge.logging.common.logging.LogEvent;
import com.omnicharge.logging.service.LogFileWriterService;
import com.omnicharge.logging.service.LogPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogEventConsumerTest {

    @Mock private LogPersistenceService logPersistenceService;
    @Mock private LogFileWriterService logFileWriterService;
    @Mock
    private com.omnicharge.logging.common.logging.LogEventPublisher logEventPublisher;


    @InjectMocks
    private LogEventConsumer logEventConsumer;

    private LogEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent = new LogEvent();
        validEvent.setServiceName("payment-service");
        validEvent.setLevel("INFO");
        validEvent.setMessage("Payment processed");
        validEvent.setTimestamp(LocalDateTime.now());
    }

    @Test
    @DisplayName("SUCCESS: Dispatches event to both file writer and persistence")
    void consumeLogEvent_Success() {
        logEventConsumer.consumeLogEvent(validEvent);

        verify(logFileWriterService, times(1)).writeToFile(validEvent);
        verify(logPersistenceService, times(1)).save(validEvent);
    }

    @Test
    @DisplayName("FAIL: File writer failure does not crash consumer")
    void consumeLogEvent_FileWriterFailure() {
        doThrow(new RuntimeException("Disk full")).when(logFileWriterService).writeToFile(any());

        assertDoesNotThrow(() -> logEventConsumer.consumeLogEvent(validEvent));
    }

    @Test
    @DisplayName("FAIL: DB persistence failure does not crash consumer")
    void consumeLogEvent_PersistenceFailure() {
        doThrow(new RuntimeException("DB timeout")).when(logPersistenceService).save(any());

        assertDoesNotThrow(() -> logEventConsumer.consumeLogEvent(validEvent));
    }
}
