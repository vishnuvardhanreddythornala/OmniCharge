package com.omnicharge.logging.repository;

import com.omnicharge.logging.entity.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    Page<LogEntry> findByServiceName(String serviceName, Pageable pageable);

    Page<LogEntry> findByLevel(String level, Pageable pageable);

    List<LogEntry> findByTraceIdOrderByTimestampAsc(String traceId);

    Page<LogEntry> findByServiceNameAndLevel(String serviceName, String level, Pageable pageable);

    @Query("SELECT l FROM LogEntry l WHERE " +
            "(:serviceName IS NULL OR l.serviceName = :serviceName) AND " +
            "(:level IS NULL OR l.level = :level) AND " +
            "(:traceId IS NULL OR l.traceId = :traceId) AND " +
            "(:startDate IS NULL OR l.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR l.timestamp <= :endDate) " +
            "ORDER BY l.timestamp DESC")
    Page<LogEntry> searchLogs(
            @Param("serviceName") String serviceName,
            @Param("level") String level,
            @Param("traceId") String traceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT l.serviceName, l.level, COUNT(l) FROM LogEntry l " +
            "WHERE l.timestamp >= :since " +
            "GROUP BY l.serviceName, l.level " +
            "ORDER BY l.serviceName, l.level")
    List<Object[]> getLogStats(@Param("since") LocalDateTime since);
}
