package com.omnicharge.logging.controller;

import com.omnicharge.logging.entity.LogEntry;
import com.omnicharge.logging.repository.LogEntryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@Tag(name = "Admin Log Management", description = "Query and search centralized logs")
public class AdminLogController {

    private final LogEntryRepository logEntryRepository;

    @GetMapping
    @Operation(summary = "Search logs with filters")
    public ResponseEntity<Page<LogEntry>> searchLogs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<LogEntry> logs = logEntryRepository.searchLogs(
                service, level, traceId, startDate, endDate,
                PageRequest.of(page, size, Sort.by("timestamp").descending()));

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/trace/{traceId}")
    @Operation(summary = "Get all logs for a specific trace (request journey)")
    public ResponseEntity<List<LogEntry>> getLogsByTrace(@PathVariable String traceId) {
        List<LogEntry> logs = logEntryRepository.findByTraceIdOrderByTimestampAsc(traceId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get log statistics per service and level")
    public ResponseEntity<List<Map<String, Object>>> getLogStats(
            @RequestParam(defaultValue = "24") int hours) {

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<Object[]> rawStats = logEntryRepository.getLogStats(since);

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Object[] row : rawStats) {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("serviceName", row[0]);
            stat.put("level", row[1]);
            stat.put("count", row[2]);
            stats.add(stat);
        }

        return ResponseEntity.ok(stats);
    }
}
