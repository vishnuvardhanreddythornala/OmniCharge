package com.omnicharge.gateway.filter;

import com.omnicharge.gateway.common.logging.LogEvent;
import com.omnicharge.gateway.common.logging.LogEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WebFluxLogCaptureFilterTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private WebFluxLogCaptureFilter webFluxLogCaptureFilter;

    @Test
    void filter_SkipsActuatorPaths() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        webFluxLogCaptureFilter.filter(exchange, filterChain).block();

        verify(filterChain, times(1)).filter(exchange);
        verifyNoInteractions(logEventPublisher);
    }

    @Test
    void filter_LogsNormalRequest() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header("X-User-Id", "1")
                .header("X-Forwarded-For", "192.168.1.1")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        Route mockRoute = mock(Route.class);
        when(mockRoute.getId()).thenReturn("user-service");
        when(mockRoute.getUri()).thenReturn(URI.create("http://localhost:8081"));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, mockRoute);

        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        webFluxLogCaptureFilter.filter(exchange, filterChain).block();

        verify(filterChain, times(1)).filter(exchange);
        // Expecting 2 log events: one for HTTP request, one for routing
        verify(logEventPublisher, times(2)).publish(any(LogEvent.class));
    }

    @Test
    void filter_LogsErrorRequest() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/err").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("Chain Broken")));

        assertDoesNotThrow(() -> {
            try {
                webFluxLogCaptureFilter.filter(exchange, filterChain).block();
            } catch (Exception ignored) {
                // Expected block to throw since mono has error
            }
        });

        // 1 event for HTTP ERROR, no routing since it crashed
        verify(logEventPublisher, times(1)).publish(argThat(logEvent -> logEvent.getLevel().equals("ERROR")));
    }
}
