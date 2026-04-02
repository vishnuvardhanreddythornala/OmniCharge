package com.omnicharge.gateway.filter;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for WebFluxLogCaptureFilter.
 * 
 * Tests cover:
 * - Request/response capture in reactive context
 * - Trace ID extraction from headers
 * - Routing decision logging
 * - Source IP extraction (X-Forwarded-For and remote address)
 * - User context extraction (X-User-Id, X-User-Role)
 * - Log level determination based on status codes
 * - Skipping actuator/swagger endpoints
 * - Error handling and stack trace capture
 * - Filter ordering
 */
@ExtendWith(MockitoExtension.class)
class WebFluxLogCaptureFilterTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private GatewayFilterChain chain;

    @Captor
    private ArgumentCaptor<LogEvent> logEventCaptor;

    private WebFluxLogCaptureFilter filter;

    @BeforeEach
    void setUp() {
        filter = new WebFluxLogCaptureFilter(logEventPublisher);
    }

    // === Basic Request/Response Capture Tests ===

    @Test
    void successfulRequest_LogsWithInfoLevel() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        // Mock route for target service extraction
        Route route = Route.async()
                .id("user-service")
                .uri(URI.create("lb://user-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("api-gateway", httpEvent.getServiceName());
        assertEquals("INFO", httpEvent.getLevel());
        assertEquals("HTTP", httpEvent.getEventType());
        assertTrue(httpEvent.getMessage().contains("GET"));
        assertTrue(httpEvent.getMessage().contains("/api/users/profile"));
        assertTrue(httpEvent.getMessage().contains("200"));
        assertNotNull(httpEvent.getContext());
        assertEquals("GET", httpEvent.getContext().get("method"));
        assertEquals("/api/users/profile", httpEvent.getContext().get("path"));
        assertEquals(200, httpEvent.getContext().get("statusCode"));
        assertEquals("user-service", httpEvent.getContext().get("targetService"));
    }

    @Test
    void clientError_LogsWithWarnLevel() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/payments/process")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);

        Route route = Route.async()
                .id("payment-service")
                .uri(URI.create("lb://payment-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("WARN", httpEvent.getLevel());
        assertTrue(httpEvent.getMessage().contains("400"));
    }

    @Test
    void serverError_LogsWithErrorLevel() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/recharges/initiate")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

        Route route = Route.async()
                .id("recharge-service")
                .uri(URI.create("lb://recharge-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("ERROR", httpEvent.getLevel());
        assertTrue(httpEvent.getMessage().contains("500"));
    }

    // === User Context Extraction Tests ===

    @Test
    void authenticatedRequest_IncludesUserContext() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/notifications")
                .header("X-User-Id", "42")
                .header("X-User-Role", "ROLE_USER")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = Route.async()
                .id("notification-service")
                .uri(URI.create("lb://notification-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertTrue(httpEvent.getMessage().contains("userId=42"));
        assertTrue(httpEvent.getMessage().contains("role=ROLE_USER"));
        assertEquals("42", httpEvent.getContext().get("userId"));
        assertEquals("ROLE_USER", httpEvent.getContext().get("userRole"));
    }

    @Test
    void unauthenticatedRequest_NoUserContext() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/operators/detect")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = Route.async()
                .id("operator-service")
                .uri(URI.create("lb://operator-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertFalse(httpEvent.getMessage().contains("userId="));
        assertNull(httpEvent.getContext().get("userId"));
    }

    // === Source IP Extraction Tests ===

    @Test
    void request_WithXForwardedFor_ExtractsFirstIp() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/plans/search")
                .header("X-Forwarded-For", "203.0.113.1, 198.51.100.1, 192.0.2.1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = Route.async()
                .id("operator-service")
                .uri(URI.create("lb://operator-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertTrue(httpEvent.getMessage().contains("sourceIp=203.0.113.1"));
        assertEquals("203.0.113.1", httpEvent.getContext().get("sourceIp"));
    }

    @Test
    void request_WithoutXForwardedFor_UsesRemoteAddress() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.100", 54321))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = Route.async()
                .id("user-service")
                .uri(URI.create("lb://user-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertTrue(httpEvent.getMessage().contains("sourceIp=192.168.1.100") ||
                   httpEvent.getMessage().contains("sourceIp=/192.168.1.100"));
    }

    // === Trace ID Extraction Tests ===

    @Test
    void request_WithTraceIdHeader_ExtractsTraceId() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/recharges/history")
                .header("X-B3-TraceId", "abc123def456")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = Route.async()
                .id("recharge-service")
                .uri(URI.create("lb://recharge-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("abc123def456", httpEvent.getTraceId());
    }

    @Test
    void request_WithAlternativeTraceIdHeader_ExtractsTraceId() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/payments/confirm")
                .header("traceid", "xyz789")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = Route.async()
                .id("payment-service")
                .uri(URI.create("lb://payment-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("xyz789", httpEvent.getTraceId());
    }

    // === Routing Decision Logging Tests ===

    @Test
    void successfulRequest_LogsRoutingDecision() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = Route.async()
                .id("user-service")
                .uri(URI.create("lb://user-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeast(2)).publish(logEventCaptor.capture());
        
        // Should have both HTTP and ROUTING events
        LogEvent routingEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "ROUTING".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("api-gateway", routingEvent.getServiceName());
        assertEquals("DEBUG", routingEvent.getLevel());
        assertEquals("ROUTING", routingEvent.getEventType());
        assertTrue(routingEvent.getMessage().contains("user-service"));
        assertTrue(routingEvent.getMessage().contains("lb://user-service"));
    }

    // === Skip Logging Tests ===

    @Test
    void actuatorEndpoint_SkipsLogging() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, never()).publish(any());
    }

    @Test
    void swaggerEndpoint_SkipsLogging() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/swagger-ui/index.html")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, never()).publish(any());
    }

    @Test
    void apiDocsEndpoint_SkipsLogging() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/v3/api-docs")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, never()).publish(any());
    }

    @Test
    void webjarsEndpoint_SkipsLogging() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/webjars/swagger-ui/index.html")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, never()).publish(any());
    }

    // === Error Handling Tests ===

    @Test
    void requestWithError_CapturesStackTrace() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/payments/process")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

        Route route = Route.async()
                .id("payment-service")
                .uri(URI.create("lb://payment-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        RuntimeException error = new RuntimeException("Payment gateway timeout");
        when(chain.filter(exchange)).thenReturn(Mono.error(error));

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .expectError(RuntimeException.class)
                .verify();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("ERROR", httpEvent.getLevel());
        assertNotNull(httpEvent.getStackTrace());
        assertTrue(httpEvent.getStackTrace().contains("RuntimeException"));
        assertTrue(httpEvent.getStackTrace().contains("Payment gateway timeout"));
    }

    // === Filter Ordering Test ===

    @Test
    void filterOrder_IsZero() {
        // Filter should run after JwtAuthenticationFilter (order = -1)
        assertEquals(0, filter.getOrder());
    }

    // === Response Time Measurement Test ===

    @Test
    void request_MeasuresResponseTime() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/operators/active")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = Route.async()
                .id("operator-service")
                .uri(URI.create("lb://operator-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertNotNull(httpEvent.getContext().get("duration"));
        assertTrue((Long) httpEvent.getContext().get("duration") >= 0);
        assertTrue(httpEvent.getMessage().contains("ms)"));
    }

    // === No Route Attribute Test ===

    @Test
    void requestWithoutRoute_HandlesGracefully() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/unknown/endpoint")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);

        // No route attribute set
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("unknown", httpEvent.getContext().get("targetService"));
        assertEquals("unknown", httpEvent.getContext().get("targetUri"));
    }

    // === Multiple HTTP Methods Test ===

    @Test
    void postRequest_LogsCorrectMethod() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/recharges/initiate")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.CREATED);

        Route route = Route.async()
                .id("recharge-service")
                .uri(URI.create("lb://recharge-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("POST", httpEvent.getContext().get("method"));
        assertTrue(httpEvent.getMessage().contains("POST"));
    }

    @Test
    void putRequest_LogsCorrectMethod() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .put("/api/users/profile")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = Route.async()
                .id("user-service")
                .uri(URI.create("lb://user-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("PUT", httpEvent.getContext().get("method"));
        assertTrue(httpEvent.getMessage().contains("PUT"));
    }

    @Test
    void deleteRequest_LogsCorrectMethod() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .delete("/api/notifications/123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);

        Route route = Route.async()
                .id("notification-service")
                .uri(URI.create("lb://notification-service"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Assert
        verify(logEventPublisher, atLeastOnce()).publish(logEventCaptor.capture());
        
        LogEvent httpEvent = logEventCaptor.getAllValues().stream()
                .filter(e -> "HTTP".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals("DELETE", httpEvent.getContext().get("method"));
        assertTrue(httpEvent.getMessage().contains("DELETE"));
    }
}
