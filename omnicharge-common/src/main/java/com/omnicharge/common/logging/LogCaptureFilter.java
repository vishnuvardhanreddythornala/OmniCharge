package com.omnicharge.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/*
 * Servlet filter that captures every HTTP request/response and publishes
 * structured log events to the centralized logging-service.
 * This filter does NOT modify terminal/console output. It runs in parallel
 * with the existing logging — the console logs remain exactly as they are.
 * Captures: method, URI, status code, response time, traceId, userId header.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Run after tracing filter but before business filters
public class LogCaptureFilter extends OncePerRequestFilter {

    private final LogEventPublisher logEventPublisher;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String traceId = MDC.get("traceId");
        String spanId = MDC.get("spanId");

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String userId = request.getHeader("X-User-Id");
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            String level = (status >= 500) ? "ERROR" : (status >= 400) ? "WARN" : "INFO";

            String message = String.format("[HTTP] %s %s → %d (%dms)%s",
                    method, uri, status, duration,
                    userId != null ? " | userId=" + userId : "");

            LogEvent event = LogEvent.builder()
                    .level(level)
                    .logger("LogCaptureFilter")
                    .message(message)
                    .traceId(traceId != null ? traceId : "")
                    .spanId(spanId != null ? spanId : "")
                    .threadName(Thread.currentThread().getName())
                    .timestamp(LocalDateTime.now())
                    .build();

            logEventPublisher.publish(event);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip actuator and swagger endpoints to avoid log noise
        return path.startsWith("/actuator") 
                || path.startsWith("/swagger") 
                || path.startsWith("/v3/api-docs")
                || path.contains("/webjars/");
    }
}
