package com.omnicharge.user.common.logging;

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
class FallbackLogReplayerTest {

    @Mock private RabbitTemplate rabbitTemplate;
    private FallbackLogReplayer replayer;
    @TempDir Path tempDir;
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        replayer = new FallbackLogReplayer(rabbitTemplate);
        ReflectionTestUtils.setField(replayer, "serviceName", "user-service");
        ReflectionTestUtils.setField(replayer, "fallbackDir", tempDir.toString());
    }

    private void invokeReplayLogs() throws Exception {
        java.lang.reflect.Method m = FallbackLogReplayer.class.getDeclaredMethod("replayLogs");
        m.setAccessible(true);
        m.invoke(replayer);
    }

    @Test
    void replayLogs_NoFiles() throws Exception {
        invokeReplayLogs();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(LogEvent.class));
    }

    @Test
    void replayLogs_AllSuccess_FileDeleted() throws Exception {
        LogEvent e = new LogEvent(); e.setServiceName("user-service"); e.setLevel("ERROR");
        e.setMessage("t"); e.setTimestamp(LocalDateTime.now());
        Path f = tempDir.resolve("fallback-buffer-user-service.log");
        Files.writeString(f, MAPPER.writeValueAsString(e) + System.lineSeparator());

        invokeReplayLogs();
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("log.user-service"), any(LogEvent.class));
        assertFalse(Files.exists(tempDir.resolve("processing-buffer-user-service.log")));
    }

    @Test
    void replayLogs_BrokerDown_FileRetained() throws Exception {
        LogEvent e = new LogEvent(); e.setServiceName("user-service"); e.setLevel("ERROR");
        e.setMessage("t"); e.setTimestamp(LocalDateTime.now());
        Path f = tempDir.resolve("fallback-buffer-user-service.log");
        Files.writeString(f, MAPPER.writeValueAsString(e) + System.lineSeparator());
        doThrow(new RuntimeException("MQ down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(LogEvent.class));

        invokeReplayLogs();
        assertTrue(Files.exists(tempDir.resolve("processing-buffer-user-service.log")));
    }

    @Test
    void replayLogs_ExistingProcessingFile() throws Exception {
        LogEvent e = new LogEvent(); e.setServiceName("user-service"); e.setLevel("INFO");
        e.setMessage("t"); e.setTimestamp(LocalDateTime.now());
        Path p = tempDir.resolve("processing-buffer-user-service.log");
        Files.writeString(p, MAPPER.writeValueAsString(e) + System.lineSeparator());

        invokeReplayLogs();
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("log.user-service"), any(LogEvent.class));
    }

    @Test
    void replayLogs_BadJson_Retained() throws Exception {
        Path f = tempDir.resolve("fallback-buffer-user-service.log");
        Files.writeString(f, "invalid json" + System.lineSeparator());
        invokeReplayLogs();
        assertTrue(Files.exists(tempDir.resolve("processing-buffer-user-service.log")));
    }

    @Test
    void destroy_ShutsDown() {
        assertDoesNotThrow(() -> replayer.destroy());
    }
}
