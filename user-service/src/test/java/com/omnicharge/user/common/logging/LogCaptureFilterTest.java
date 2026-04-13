package com.omnicharge.user.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class LogCaptureFilterTest {
    @Mock private LogEventPublisher logEventPublisher;
    @InjectMocks private LogCaptureFilter filter;

    @Test
    void testDoFilter() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api");
        when(res.getStatus()).thenReturn(200);
        
        filter.doFilter(req, res, chain);
        verify(chain, atLeastOnce()).doFilter(req, res);
    }
}
