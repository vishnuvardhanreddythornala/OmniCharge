package com.omnicharge.payment.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class LogCaptureFilterTest {

    @Mock private LogEventPublisher logEventPublisher;
    @InjectMocks private LogCaptureFilter filter;
    
    private HttpServletRequest req;
    private HttpServletResponse res;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api/test");
        MDC.clear();
    }

    @Test
    void testDoFilter_Success_Status200() throws Exception {
        when(res.getStatus()).thenReturn(200);
        when(req.getHeader("X-User-Id")).thenReturn("user123");
        MDC.put("traceId", "trace-1");
        MDC.put("spanId", "span-1");
        
        filter.doFilterInternal(req, res, chain);
        
        verify(chain, times(1)).doFilter(req, res);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "INFO".equals(e.getLevel())));
    }

    @Test
    void testDoFilter_Warn_Status400() throws Exception {
        when(res.getStatus()).thenReturn(400);
        when(req.getHeader("X-User-Id")).thenReturn(null);
        
        filter.doFilterInternal(req, res, chain);
        
        verify(chain, times(1)).doFilter(req, res);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "WARN".equals(e.getLevel())));
    }

    @Test
    void testDoFilter_Error_Status500() throws Exception {
        when(res.getStatus()).thenReturn(500);
        
        filter.doFilterInternal(req, res, chain);
        
        verify(chain, times(1)).doFilter(req, res);
        verify(logEventPublisher, times(1)).publish(argThat(e -> "ERROR".equals(e.getLevel())));
    }

    @Test
    void testDoFilter_ExceptionThrown() throws Exception {
        when(res.getStatus()).thenReturn(500);
        doThrow(new RuntimeException("Chain error")).when(chain).doFilter(req, res);
        
        assertThrows(RuntimeException.class, () -> filter.doFilterInternal(req, res, chain));
        verify(logEventPublisher, times(1)).publish(any());
    }


    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "/actuator/health",
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/webjars/springfox-swagger-ui"
    })
    void testShouldNotFilter_ExcludedPaths(String uri) {
        when(req.getRequestURI()).thenReturn(uri);
        assertTrue(filter.shouldNotFilter(req));
    }


    @Test
    void testShouldNotFilter_NormalPath() {
        when(req.getRequestURI()).thenReturn("/api/users");
        assertFalse(filter.shouldNotFilter(req));
    }
}
