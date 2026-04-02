package com.omnicharge.logging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.logging.entity.LogEntry;
import com.omnicharge.logging.repository.LogEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Persists log events to the MySQL database.
 * Populates eventType and contextJson fields for enhanced querying (Requirements 1.1-1.8).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogPersistenceService {

    private final LogEntryRepository logEntryRepository;
    private final ObjectMapper objectMapper;

    public void save(LogEvent event) {
        try {
            // Convert context map to JSON string if present
            String contextJson = null;
            if (event.getContext() != null && !event.getContext().isEmpty()) {
                try {
                    contextJson = objectMapper.writeValueAsString(event.getContext());
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize context map for event from {}: {}", 
                            event.getServiceName(), e.getMessage());
                }
            }
            
            LogEntry entry = LogEntry.builder()
                    .serviceName(event.getServiceName())
                    .level(event.getLevel())
                    .logger(event.getLogger())
                    .message(event.getMessage())
                    .traceId(event.getTraceId())
                    .spanId(event.getSpanId())
                    .threadName(event.getThreadName())
                    .stackTrace(event.getStackTrace())
                    .timestamp(event.getTimestamp())
                    .eventType(event.getEventType())
                    .contextJson(contextJson)
                    .build();

            logEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to persist log entry from {}: {}", event.getServiceName(), e.getMessage());
        }
    }
}
