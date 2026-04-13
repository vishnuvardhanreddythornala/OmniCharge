package com.omnicharge.gateway.filter;

import com.omnicharge.gateway.common.logging.LogEvent;
import com.omnicharge.gateway.common.logging.LogEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebFlux-compatible logging filter for API Gateway.
 * Captures HTTP requests/responses in reactive environment and publishes
 * structured log events to the centralized logging system.
 * 
 * Logs:
 * - HTTP method, path, source IP
 * - Target service and routing decision
 * - Response status code and response time
 * - Trace ID from reactive context
 * 
 * This filter runs AFTER JwtAuthenticationFilter (order = 0) to capture
 * the complete request lifecycle including authentication results.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebFluxLogCaptureFilter implements GlobalFilter, Ordered {

    private final LogEventPublisher logEventPublisher;

    @Override
    @SuppressWarnings("java:S3358")
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // Skip logging for actuator and internal endpoints to avoid noise
        if (shouldSkipLogging(path)) {
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();
        
        // Extract request details
        String method = request.getMethod().name();
        String sourceIp = extractSourceIp(request);
        String userId = request.getHeaders().getFirst("X-User-Id");
        String userRole = request.getHeaders().getFirst("X-User-Role");
        
        // Extract trace ID from reactive context (Micrometer tracing)
        return chain.filter(exchange)
                .doOnError(error -> logRequest(exchange, method, path, sourceIp, userId, userRole, startTime, error))
                .then(Mono.fromRunnable(() -> logRequest(exchange, method, path, sourceIp, userId, userRole, startTime, null)));
    }

    private void logRequest(ServerWebExchange exchange, String method, String path, 
                           String sourceIp, String userId, String userRole, 
                           long startTime, Throwable error) {
        try {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;
            
            // Get response status
            int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 500;
            
            // Extract target service from route
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String targetService = route != null ? route.getId() : "unknown";
            String targetUri = route != null ? route.getUri().toString() : "unknown";
            
            // Determine log level based on status code
            String level = determineLogLevel(statusCode, error);
            
            // Build log message
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(String.format("[API-GATEWAY] %s %s → %d (%dms)", 
                method, path, statusCode, duration));
            
            if (userId != null) {
                messageBuilder.append(" | userId=").append(userId);
            }
            if (userRole != null) {
                messageBuilder.append(" | role=").append(userRole);
            }
            messageBuilder.append(" | sourceIp=").append(sourceIp);
            messageBuilder.append(" | targetService=").append(targetService);
            
            if (error != null) {
                messageBuilder.append(" | error=").append(error.getMessage());
            }
            
            // Build context map
            Map<String, Object> context = new HashMap<>();
            context.put("method", method);
            context.put("path", path);
            context.put("statusCode", statusCode);
            context.put("duration", duration);
            context.put("sourceIp", sourceIp);
            context.put("targetService", targetService);
            context.put("targetUri", targetUri);
            if (userId != null) {
                context.put("userId", userId);
            }
            if (userRole != null) {
                context.put("userRole", userRole);
            }
            
            // Extract trace ID from Micrometer context if available
            String traceId = extractTraceId(exchange);
            
            // Create and publish log event
            LogEvent logEvent = LogEvent.builder()
                    .serviceName("api-gateway")
                    .level(level)
                    .eventType("HTTP")
                    .logger(WebFluxLogCaptureFilter.class.getName())
                    .message(messageBuilder.toString())
                    .traceId(traceId != null ? traceId : "")
                    .spanId("")
                    .threadName(Thread.currentThread().getName())
                    .timestamp(LocalDateTime.now())
                    .context(context)
                    .stackTrace(error != null ? getStackTrace(error) : null)
                    .build();
            
            logEventPublisher.publish(logEvent);
            
            // Also log routing decision at DEBUG level for detailed analysis
            if (!"unknown".equals(targetService)) {
                logRoutingDecision(method, path, targetService, targetUri, traceId);
            }
            
        } catch (Exception e) {
            // Don't let logging errors break the request flow
            log.error("Error logging request: {}", e.getMessage(), e);
        }
    }

    private void logRoutingDecision(String method, String path, String targetService, 
                                    String targetUri, String traceId) {
        String message = String.format("[ROUTING] %s %s → %s (%s)", 
            method, path, targetService, targetUri);
        
        Map<String, Object> context = new HashMap<>();
        context.put("method", method);
        context.put("path", path);
        context.put("targetService", targetService);
        context.put("targetUri", targetUri);
        
        LogEvent routingEvent = LogEvent.builder()
                .serviceName("api-gateway")
                .level("DEBUG")
                .eventType("ROUTING")
                .logger(WebFluxLogCaptureFilter.class.getName())
                .message(message)
                .traceId(traceId != null ? traceId : "")
                .timestamp(LocalDateTime.now())
                .context(context)
                .build();
        
        logEventPublisher.publish(routingEvent);
    }

    private String extractSourceIp(ServerHttpRequest request) {
        // Try X-Forwarded-For header first (for proxied requests)
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return forwardedFor.split(",")[0].trim();
        }
        
        // Fall back to remote address
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        
        return "unknown";
    }

    private String extractTraceId(ServerWebExchange exchange) {
        // Try to extract from Micrometer tracing context
        // This will be populated by Spring Cloud Sleuth/Micrometer if configured
        try {
            // Check for trace ID in request headers (propagated from upstream)
            String traceId = exchange.getRequest().getHeaders().getFirst("X-B3-TraceId");
            if (traceId != null && !traceId.isEmpty()) {
                return traceId;
            }
            
            // Alternative header names
            traceId = exchange.getRequest().getHeaders().getFirst("traceid");
            if (traceId != null && !traceId.isEmpty()) {
                return traceId;
            }
            
            // Could also check exchange attributes for Micrometer context
            // but that requires additional dependencies
        } catch (Exception e) {
            log.debug("Could not extract trace ID: {}", e.getMessage());
        }
        
        return null;
    }

    private String determineLogLevel(int statusCode, Throwable error) {
        if (error != null) {
            return "ERROR";
        }
        if (statusCode >= 500) {
            return "ERROR";
        }
        if (statusCode >= 400) {
            return "WARN";
        }
        return "INFO";
    }

    private boolean shouldSkipLogging(String path) {
        return path.startsWith("/actuator") 
            || path.startsWith("/swagger") 
            || path.startsWith("/v3/api-docs")
            || path.contains("/webjars/");
    }

    private String getStackTrace(Throwable error) {
        if (error == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(error.getClass().getName()).append(": ").append(error.getMessage()).append("\n");
        
        for (StackTraceElement element : error.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            // Limit stack trace to first 10 elements to avoid huge logs
            if (sb.length() > 2000) {
                sb.append("\t... (truncated)");
                break;
            }
        }
        
        return sb.toString();
    }

    @Override
    public int getOrder() {
        // Run after JwtAuthenticationFilter (order = -1) to capture auth results
        return 0;
    }
}
