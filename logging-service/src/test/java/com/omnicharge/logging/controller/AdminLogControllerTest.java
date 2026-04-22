package com.omnicharge.logging.controller;

import com.omnicharge.logging.entity.LogEntry;
import com.omnicharge.logging.repository.LogEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AdminLogControllerTest {

    @Mock private LogEntryRepository logEntryRepository;
    @InjectMocks private AdminLogController controller;

    @Test
    void searchLogs_Success() {
        Page<LogEntry> page = new PageImpl<>(Collections.emptyList());
        when(logEntryRepository.searchLogs(any(), any(), any(), any(), any(), any())).thenReturn(page);

        ResponseEntity<Page<LogEntry>> response = controller.searchLogs(null, null, null, null, null, 0, 50);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getLogsByTrace_Success() {
        when(logEntryRepository.findByTraceIdOrderByTimestampAsc("trace-123")).thenReturn(List.of());

        ResponseEntity<List<LogEntry>> response = controller.getLogsByTrace("trace-123");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getLogStats_Success() {
        List<Object[]> rawStats = new ArrayList<>();
        rawStats.add(new Object[]{"payment-service", "INFO", 42L});
        when(logEntryRepository.getLogStats(any())).thenReturn(rawStats);

        ResponseEntity<List<Map<String, Object>>> response = controller.getLogStats(24);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("payment-service", response.getBody().get(0).get("serviceName"));
    }

    @Test
    void getLogStats_EmptyResults() {
        when(logEntryRepository.getLogStats(any())).thenReturn(new ArrayList<>());

        ResponseEntity<List<Map<String, Object>>> response = controller.getLogStats(48);

        assertNotNull(response);
        assertTrue(response.getBody().isEmpty());
    }
}
