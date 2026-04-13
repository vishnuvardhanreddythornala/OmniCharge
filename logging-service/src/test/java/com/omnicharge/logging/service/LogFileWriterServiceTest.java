package com.omnicharge.logging.service;

import com.omnicharge.logging.common.logging.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LogFileWriterServiceTest {

    @InjectMocks private LogFileWriterService service;
    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "logBaseDir", tempDir.toString());
    }

    private LogEvent buildEvent(String service, String level, String message) {
        LogEvent event = new LogEvent();
        event.setServiceName(service);
        event.setLevel(level);
        event.setMessage(message);
        event.setTimestamp(LocalDateTime.now());
        event.setTraceId("trace-123");
        event.setSpanId("span-456");
        event.setThreadName("main");
        event.setLogger("com.test.Logger");
        return event;
    }

    // ===== per-service log writing =====

    @Test
    void writeToFile_PerServiceLog() throws IOException {
        LogEvent event = buildEvent("payment-service", "INFO", "test info");

        service.writeToFile(event);

        Path serviceLog = tempDir.resolve("payment-service").resolve("payment-service.log");
        assertTrue(Files.exists(serviceLog));
        String content = Files.readString(serviceLog);
        assertTrue(content.contains("test info"));
        assertTrue(content.contains("payment-service"));
        assertTrue(content.contains("trace-123"));
    }

    // ===== selective filtering for all-services.log =====

    @Test
    void writeToFile_ErrorLevel_WritesToAllServices() throws IOException {
        LogEvent event = buildEvent("user-service", "ERROR", "critical failure");

        service.writeToFile(event);

        Path allServicesLog = tempDir.resolve("all-services.log");
        assertTrue(Files.exists(allServicesLog));
        assertTrue(Files.readString(allServicesLog).contains("critical failure"));
    }

    @Test
    void writeToFile_WarnLevel_WritesToAllServices() throws IOException {
        LogEvent event = buildEvent("user-service", "WARN", "warning msg");

        service.writeToFile(event);

        Path allServicesLog = tempDir.resolve("all-services.log");
        assertTrue(Files.exists(allServicesLog));
        assertTrue(Files.readString(allServicesLog).contains("warning msg"));
    }

    @Test
    void writeToFile_LifecycleEvent_WritesToAllServices() throws IOException {
        LogEvent event = buildEvent("user-service", "INFO", "lifecycle event");
        event.setEventType("LIFECYCLE");

        service.writeToFile(event);

        Path allServicesLog = tempDir.resolve("all-services.log");
        assertTrue(Files.exists(allServicesLog));
    }

    @Test
    void writeToFile_InfoLevel_DoesNotWriteToAllServices() throws IOException {
        LogEvent event = buildEvent("user-service", "INFO", "normal info");

        service.writeToFile(event);

        Path allServicesLog = tempDir.resolve("all-services.log");
        assertFalse(Files.exists(allServicesLog));
    }

    @Test
    void writeToFile_DebugLevel_DoesNotWriteToAllServices() throws IOException {
        LogEvent event = buildEvent("user-service", "DEBUG", "debug msg");

        service.writeToFile(event);

        Path allServicesLog = tempDir.resolve("all-services.log");
        assertFalse(Files.exists(allServicesLog));
    }

    // ===== formatLogLine branches =====

    @Test
    void writeToFile_NullTimestamp() throws IOException {
        LogEvent event = buildEvent("test-svc", "INFO", "no timestamp");
        event.setTimestamp(null);

        service.writeToFile(event);

        Path serviceLog = tempDir.resolve("test-svc").resolve("test-svc.log");
        String content = Files.readString(serviceLog);
        assertTrue(content.contains("N/A"));
    }

    @Test
    void writeToFile_NullTraceAndSpan() throws IOException {
        LogEvent event = buildEvent("test-svc", "ERROR", "no trace");
        event.setTraceId(null);
        event.setSpanId(null);

        service.writeToFile(event);

        Path serviceLog = tempDir.resolve("test-svc").resolve("test-svc.log");
        String content = Files.readString(serviceLog);
        assertTrue(content.contains(",-,"));
    }

    @Test
    void writeToFile_NullThreadNameAndLogger() throws IOException {
        LogEvent event = buildEvent("test-svc", "WARN", "no thread");
        event.setThreadName(null);
        event.setLogger(null);

        service.writeToFile(event);

        Path serviceLog = tempDir.resolve("test-svc").resolve("test-svc.log");
        String content = Files.readString(serviceLog);
        assertTrue(content.contains("-"));
    }

    @Test
    void writeToFile_WithStackTrace() throws IOException {
        LogEvent event = buildEvent("test-svc", "ERROR", "exception occurred");
        event.setStackTrace("java.lang.NullPointerException\n\tat com.test.Foo.bar(Foo.java:42)");

        service.writeToFile(event);

        Path serviceLog = tempDir.resolve("test-svc").resolve("test-svc.log");
        String content = Files.readString(serviceLog);
        assertTrue(content.contains("NullPointerException"));
    }

    @Test
    void writeToFile_NullLevel_NotWrittenToAllServices() throws IOException {
        LogEvent event = buildEvent("test-svc", null, "null level");

        service.writeToFile(event);

        Path allServicesLog = tempDir.resolve("all-services.log");
        assertFalse(Files.exists(allServicesLog));
    }

    // ===== file rolling =====

    @Test
    void writeToFile_FileRolling_WhenSizeExceeds10MB() throws IOException {
        // Create a file that's already at the size limit
        Path serviceDir = tempDir.resolve("big-service");
        Files.createDirectories(serviceDir);
        Path logFile = serviceDir.resolve("big-service.log");

        // Write 10MB+ of data
        byte[] tenMb = new byte[10 * 1024 * 1024 + 1];
        java.util.Arrays.fill(tenMb, (byte) 'X');
        Files.write(logFile, tenMb);

        LogEvent event = buildEvent("big-service", "INFO", "after rolling");

        service.writeToFile(event);

        // Original file should have been rolled and new content written
        String content = Files.readString(logFile);
        assertTrue(content.contains("after rolling"));
    }
}
