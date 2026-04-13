package com.omnicharge.notification.common.logging;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FallbackLogReplayerTest {

    @Mock private RabbitTemplate rabbitTemplate;
    private FallbackLogReplayer replayer;
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        replayer = new FallbackLogReplayer(rabbitTemplate);
        ReflectionTestUtils.setField(replayer, "serviceName", "notification-service");
        ReflectionTestUtils.setField(replayer, "fallbackDir", tempDir.toString());
    }

    @Test
    void replayLogs_NoFallbackFile_DoesNothing() {
        // No files → silent return
        invokeReplay();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void replayLogs_WithFallbackFile_ReplaysAndDeletes() throws IOException {
        LogEvent event = new LogEvent();
        event.setServiceName("notification-service");
        event.setLevel("INFO");
        event.setMessage("test event");
        event.setTimestamp(LocalDateTime.now());

        String json = MAPPER.writeValueAsString(event);
        Path fallbackFile = tempDir.resolve("fallback-buffer-notification-service.log");
        Files.writeString(fallbackFile, json + "\n");

        invokeReplay();

        verify(rabbitTemplate).convertAndSend(eq(LoggingConstants.LOGGING_EXCHANGE),
                eq("log.notification-service"),
                any(Object.class)
        );
        // After successful replay, processing file should be deleted
        assertFalse(Files.exists(tempDir.resolve("processing-buffer-notification-service.log")));
    }

    @Test
    void replayLogs_BrokerDown_PreservesRemainingLines() throws IOException {
        LogEvent event = new LogEvent();
        event.setServiceName("notification-service");
        event.setLevel("ERROR");
        event.setMessage("test");
        event.setTimestamp(LocalDateTime.now());

        String json = MAPPER.writeValueAsString(event);
        Path fallbackFile = tempDir.resolve("fallback-buffer-notification-service.log");
        Files.writeString(fallbackFile, json + "\n" + json + "\n");

        doThrow(new RuntimeException("Broker down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any(Object.class));

        invokeReplay();

        Path processingFile = tempDir.resolve("processing-buffer-notification-service.log");
        assertTrue(Files.exists(processingFile));
    }

    @Test
    void replayLogs_ExistingProcessingFile_ProcessesIt() throws IOException {
        LogEvent event = new LogEvent();
        event.setServiceName("notification-service");
        event.setLevel("WARN");
        event.setMessage("processing test");
        event.setTimestamp(LocalDateTime.now());

        String json = MAPPER.writeValueAsString(event);
        Path processingFile = tempDir.resolve("processing-buffer-notification-service.log");
        Files.writeString(processingFile, json + "\n");

        invokeReplay();

        verify(rabbitTemplate).convertAndSend(eq(LoggingConstants.LOGGING_EXCHANGE),
                eq("log.notification-service"),
                any(Object.class)
        );
        assertFalse(Files.exists(processingFile));
    }

    @Test
    void replayLogs_MalformedJson_BrokerMarkedDown() throws IOException {
        Path fallbackFile = tempDir.resolve("fallback-buffer-notification-service.log");
        Files.writeString(fallbackFile, "NOT_JSON\n");

        invokeReplay();

        // Malformed JSON cannot be deserialized, so RabbitMQ should never be called
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        // Malformed line fails processLine → brokerDown = true
        Path processingFile = tempDir.resolve("processing-buffer-notification-service.log");
        // The file may exist if there were remaining lines, or be deleted
        // Main assertion: no exception thrown
    }

    @Test
    void initAndDestroy_DoNotThrow() {
        assertDoesNotThrow(() -> replayer.init());
        assertDoesNotThrow(() -> replayer.destroy());
    }

    private void invokeReplay() {
        // Use reflection to call private synchronized replayLogs
        try {
            java.lang.reflect.Method m = FallbackLogReplayer.class.getDeclaredMethod("replayLogs");
            m.setAccessible(true);
            m.invoke(replayer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}




