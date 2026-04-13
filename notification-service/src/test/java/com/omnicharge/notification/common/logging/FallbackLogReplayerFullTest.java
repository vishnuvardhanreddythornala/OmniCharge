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
        ReflectionTestUtils.setField(replayer, "serviceName", "notification-service");
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
        event.setServiceName("notification-service");
        event.setLevel("ERROR");
        event.setMessage("test");
        event.setTimestamp(LocalDateTime.now());

        Path fallbackFile = tempDir.resolve("fallback-buffer-notification-service.log");
        Files.writeString(fallbackFile, MAPPER.writeValueAsString(event) + System.lineSeparator());

        invokeReplayLogs();

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("log.notification-service"), any(LogEvent.class));
        assertFalse(Files.exists(tempDir.resolve("processing-buffer-notification-service.log")));
    }

    @Test
    void replayLogs_BrokerDown_FileRetained() throws Exception {
        LogEvent event = new LogEvent();
        event.setServiceName("notification-service");
        event.setLevel("ERROR");
        event.setMessage("test");
        event.setTimestamp(LocalDateTime.now());

        Path fallbackFile = tempDir.resolve("fallback-buffer-notification-service.log");
        Files.writeString(fallbackFile, MAPPER.writeValueAsString(event) + System.lineSeparator());

        doThrow(new RuntimeException("MQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(LogEvent.class));

        invokeReplayLogs();

        assertTrue(Files.exists(tempDir.resolve("processing-buffer-notification-service.log")));
    }

    @Test
    void replayLogs_BadJson_Retained() throws Exception {
        Path fallbackFile = tempDir.resolve("fallback-buffer-notification-service.log");
        Files.writeString(fallbackFile, "invalid json" + System.lineSeparator());

        invokeReplayLogs();

        assertTrue(Files.exists(tempDir.resolve("processing-buffer-notification-service.log")));
    }

    @Test
    void destroy_ShutsDown() {
        assertDoesNotThrow(() -> replayer.destroy());
    }
}
