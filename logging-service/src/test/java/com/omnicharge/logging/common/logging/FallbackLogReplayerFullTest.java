package com.omnicharge.logging.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class FallbackLogReplayerFullTest {

    @Mock private RabbitTemplate rabbitTemplate;
    private FallbackLogReplayer replayer;
    @TempDir Path tempDir;
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        replayer = new FallbackLogReplayer(rabbitTemplate);
        ReflectionTestUtils.setField(replayer, "serviceName", "logging-service");
        ReflectionTestUtils.setField(replayer, "fallbackDir", tempDir.toString());
    }

    private void invokeReplayLogs() throws Exception {
        java.lang.reflect.Method method = FallbackLogReplayer.class.getDeclaredMethod("replayLogs");
        method.setAccessible(true);
        method.invoke(replayer);
    }

    @Test
    void replayLogs_NoFiles() throws Exception {
        invokeReplayLogs();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(LogEvent.class));
    }

    @Test
    void replayLogs_AllSuccess_FileDeleted() throws Exception {
        LogEvent event = new LogEvent();
        event.setServiceName("logging-service");
        event.setLevel("ERROR");
        event.setMessage("test");
        event.setTimestamp(LocalDateTime.now());

        Path fallbackFile = tempDir.resolve("fallback-buffer-logging-service.log");
        Files.writeString(fallbackFile, MAPPER.writeValueAsString(event) + System.lineSeparator());

        invokeReplayLogs();

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("log.logging-service"), any(LogEvent.class));
        assertFalse(Files.exists(tempDir.resolve("processing-buffer-logging-service.log")));
    }

    @Test
    void replayLogs_QueueFailure_FileRetained() throws Exception {
        LogEvent event = new LogEvent();
        event.setServiceName("logging-service");
        event.setLevel("ERROR");
        event.setMessage("test");
        event.setTimestamp(LocalDateTime.now());

        Path fallbackFile = tempDir.resolve("fallback-buffer-logging-service.log");
        Files.writeString(fallbackFile, MAPPER.writeValueAsString(event) + System.lineSeparator());

        doThrow(new RuntimeException("Queue failure")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(LogEvent.class));

        invokeReplayLogs();

        assertTrue(Files.exists(tempDir.resolve("processing-buffer-logging-service.log")));
    }

    @Test
    void replayLogs_MultipleLines_PartialFailure() throws Exception {
        LogEvent event = new LogEvent();
        event.setServiceName("logging-service");
        event.setLevel("ERROR");
        event.setMessage("test");
        event.setTimestamp(LocalDateTime.now());

        String jsonLine = MAPPER.writeValueAsString(event);
        Path fallbackFile = tempDir.resolve("fallback-buffer-logging-service.log");
        Files.writeString(fallbackFile,
                jsonLine + System.lineSeparator() + jsonLine + System.lineSeparator());

        // First call succeeds, second fails
        doNothing().doThrow(new RuntimeException("Queue down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(LogEvent.class));

        invokeReplayLogs();

        // Processing file should be retained with the second line
        Path processingFile = tempDir.resolve("processing-buffer-logging-service.log");
        assertTrue(Files.exists(processingFile));
    }

    @Test
    void replayLogs_BadJson_Retained() throws Exception {
        Path fallbackFile = tempDir.resolve("fallback-buffer-logging-service.log");
        Files.writeString(fallbackFile, "invalid json line" + System.lineSeparator());

        invokeReplayLogs();

        assertTrue(Files.exists(tempDir.resolve("processing-buffer-logging-service.log")));
    }

    @Test
    void destroy_ShutsDown() {
        assertDoesNotThrow(() -> replayer.destroy());
    }
}
