package com.omnicharge.logging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.logging.common.logging.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class FallbackReplayServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FallbackReplayService fallbackReplayService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp()  {
        ReflectionTestUtils.setField(fallbackReplayService, "fallbackDir", tempDir.toString());
    }

    @Test
    void replayFallbackLogs_SuccessDeletesFile() throws Exception {
        LogEvent mockEvent = LogEvent.builder()
                .serviceName("test-service")
                .message("Test Msg")
                .level("INFO")
                .timestamp(LocalDateTime.now())
                .build();

        Path fallbackFile = tempDir.resolve("fallback-buffer-1.log");
        Files.writeString(fallbackFile, "{\"serviceName\":\"test-service\"}\n");

        when(objectMapper.readValue(anyString(), eq(LogEvent.class))).thenReturn(mockEvent);

        fallbackReplayService.replayFallbackLogs();

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("log.test-service"), any(LogEvent.class));
        assertFalse(Files.exists(fallbackFile));
    }

    @Test
    void replayFallbackLogs_PartialFailureRetainsFile() throws Exception {
        Path fallbackFile = tempDir.resolve("fallback-buffer-2.log");
        Files.writeString(fallbackFile, "{\"valid\":\"json\"}\n{\"invalid-json\"}\n");

        LogEvent mockEvent = LogEvent.builder().serviceName("test-service").build();

        // First read succeeds, second throws exception
        when(objectMapper.readValue(anyString(), eq(LogEvent.class)))
                .thenReturn(mockEvent)
                .thenThrow(new RuntimeException("Parse Error"));

        fallbackReplayService.replayFallbackLogs();

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("log.test-service"), any(LogEvent.class));
        assertTrue(Files.exists(fallbackFile));
    }
    
    @Test
    void replayFallbackLogs_IgnoresEmptyLinesAndUnknownFiles() throws Exception {
        Path fallbackFile = tempDir.resolve("fallback-buffer-3.log");
        Files.writeString(fallbackFile, "\n   \n");
        Path otherFile = tempDir.resolve("some-other-file.log");
        Files.writeString(otherFile, "{\"serviceName\":\"test-service\"}\n");

        fallbackReplayService.replayFallbackLogs();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(LogEvent.class));
        assertTrue(Files.exists(fallbackFile)); // Never successfully replayed any lines
        assertTrue(Files.exists(otherFile));
    }
}
