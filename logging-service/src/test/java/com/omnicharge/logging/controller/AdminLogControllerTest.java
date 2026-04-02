package com.omnicharge.logging.controller;

import com.omnicharge.logging.entity.LogEntry;
import com.omnicharge.logging.repository.LogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminLogController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class AdminLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogEntryRepository logEntryRepository;

    private LogEntry sampleEntry;

    @BeforeEach
    void setUp() {
        sampleEntry = LogEntry.builder()
                .id(1L)
                .serviceName("user-service")
                .level("ERROR")
                .logger("UserController")
                .message("User not found")
                .traceId("traceXYZ")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void searchLogs_ReturnsPageOfLogs() throws Exception {
        Page<LogEntry> page = new PageImpl<>(List.of(sampleEntry));
        
        when(logEntryRepository.searchLogs(
                eq("user-service"), eq("ERROR"), any(), any(), any(), any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/admin/logs")
                .param("service", "user-service")
                .param("level", "ERROR")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].serviceName").value("user-service"))
                .andExpect(jsonPath("$.content[0].level").value("ERROR"))
                .andExpect(jsonPath("$.content[0].message").value("User not found"));
    }

    @Test
    void getLogsByTrace_ReturnsLogsForTraceId() throws Exception {
        when(logEntryRepository.findByTraceIdOrderByTimestampAsc("traceXYZ"))
                .thenReturn(List.of(sampleEntry));

        mockMvc.perform(get("/api/admin/logs/trace/traceXYZ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].traceId").value("traceXYZ"));
    }

    @Test
    void getLogStats_ReturnsAggregatedStats() throws Exception {
        Object[] row1 = {"user-service", "ERROR", 5L};
        Object[] row2 = {"payment-service", "INFO", 100L};
        List<Object[]> queryResult = Arrays.asList(row1, row2);

        when(logEntryRepository.getLogStats(any(LocalDateTime.class))).thenReturn(queryResult);

        mockMvc.perform(get("/api/admin/logs/stats")
                .param("hours", "12")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("user-service"))
                .andExpect(jsonPath("$[0].count").value(5))
                .andExpect(jsonPath("$[1].serviceName").value("payment-service"))
                .andExpect(jsonPath("$[1].count").value(100));
    }
}
