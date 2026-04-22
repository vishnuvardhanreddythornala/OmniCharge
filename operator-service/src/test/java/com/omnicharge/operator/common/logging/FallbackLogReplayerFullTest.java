package com.omnicharge.operator.common.logging;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class FallbackLogReplayerFullTest {

    @Mock private RabbitTemplate rabbitTemplate;

    private FallbackLogReplayer replayer;

    @TempDir
    Path tempDir;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp()  {
        replayer = new FallbackLogReplayer(rabbitTemplate);
        ReflectionTestUtils.setField(replayer, "serviceName", "test-service");
        ReflectionTestUtils.setField(replayer, "fallbackDir", tempDir.toString());
    }

    @Test
    void replayLogs_NoFiles_DoesNothing() throws Exception {
        // Invoke via reflection since it's private
        java.lang.reflect.Method method = FallbackLogReplayer.class.getDeclaredMethod("replayLogs");
        method.setAccessible(true);
        method.invoke(replayer);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(LogEvent.class));
    }

    @Test
    void replayLogs_WithFallbackFile_AllSuccess() throws Exception {
        LogEvent event = new LogEvent();
        event.setServiceName("test-service");
        event.setLevel("ERROR");
        event.setMessage("test message");
        event.setTimestamp(LocalDateTime.now());

        String jsonLine = MAPPER.writeValueAsString(event);
        Path fallbackFile = tempDir.resolve("fallback-buffer-test-service.log");
        Files.writeString(fallbackFile, jsonLine + System.lineSeparator());

        java.lang.reflect.Method method = FallbackLogReplayer.class.getDeclaredMethod("replayLogs");
        method.setAccessible(true);
        method.invoke(replayer);

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("log.test-service"), any(LogEvent.class));
        // File should be cleaned up on success
        assertFalse(Files.exists(tempDir.resolve("processing-buffer-test-service.log")));
    }

    @Test
    void replayLogs_WithFallbackFile_BrokerDown() throws Exception {
        LogEvent event = new LogEvent();
        event.setServiceName("test-service");
        event.setLevel("ERROR");
        event.setMessage("test");
        event.setTimestamp(LocalDateTime.now());

        String jsonLine = MAPPER.writeValueAsString(event);
        Path fallbackFile = tempDir.resolve("fallback-buffer-test-service.log");
        Files.writeString(fallbackFile, jsonLine + System.lineSeparator()
                + jsonLine + System.lineSeparator());

        doThrow(new RuntimeException("MQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(LogEvent.class));

        java.lang.reflect.Method method = FallbackLogReplayer.class.getDeclaredMethod("replayLogs");
        method.setAccessible(true);
        method.invoke(replayer);

        // Processing file should still exist (retained for retry)
        Path processingFile = tempDir.resolve("processing-buffer-test-service.log");
        assertTrue(Files.exists(processingFile));
    }

    @Test
    void replayLogs_WithExistingProcessingFile() throws Exception {
        LogEvent event = new LogEvent();
        event.setServiceName("test-service");
        event.setLevel("INFO");
        event.setMessage("test");
        event.setTimestamp(LocalDateTime.now());

        String jsonLine = MAPPER.writeValueAsString(event);
        Path processingFile = tempDir.resolve("processing-buffer-test-service.log");
        Files.writeString(processingFile, jsonLine + System.lineSeparator());

        java.lang.reflect.Method method = FallbackLogReplayer.class.getDeclaredMethod("replayLogs");
        method.setAccessible(true);
        method.invoke(replayer);

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("log.test-service"), any(LogEvent.class));
    }

    @Test
    void replayLogs_BadJsonLine_MarksAsBrokerDown() throws Exception {
        Path fallbackFile = tempDir.resolve("fallback-buffer-test-service.log");
        Files.writeString(fallbackFile, "invalid json line" + System.lineSeparator());

        java.lang.reflect.Method method = FallbackLogReplayer.class.getDeclaredMethod("replayLogs");
        method.setAccessible(true);
        method.invoke(replayer);

        // Should retain unparseable line in processing file
        Path processingFile = tempDir.resolve("processing-buffer-test-service.log");
        assertTrue(Files.exists(processingFile));
    }

    @Test
    void destroy_ShutsDownScheduler() {
        assertDoesNotThrow(() -> replayer.destroy());
    }
}
