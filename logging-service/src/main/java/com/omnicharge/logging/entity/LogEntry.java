package com.omnicharge.logging.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_entries", indexes = {
        @Index(name = "idx_service_name", columnList = "serviceName"),
        @Index(name = "idx_level", columnList = "level"),
        @Index(name = "idx_trace_id", columnList = "traceId"),
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_event_type", columnList = "eventType")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String serviceName;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(length = 255)
    private String logger;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 64)
    private String traceId;

    @Column(length = 64)
    private String spanId;

    @Column(length = 100)
    private String threadName;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @CreationTimestamp
    private LocalDateTime createdDate;
    
    // New fields for enhanced logging (Requirements 1.1-1.8)
    @Column(length = 50)
    private String eventType;  // "LIFECYCLE", "RABBITMQ", "REDIS", "EXCEPTION", "BUSINESS", etc.
    
    @Column(columnDefinition = "TEXT")
    private String contextJson;  // JSON string of additional contextual data
}
