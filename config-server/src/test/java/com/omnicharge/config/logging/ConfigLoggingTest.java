package com.omnicharge.config.logging;

import com.omnicharge.config.common.logging.LogEvent;
import com.omnicharge.config.common.logging.LogEventPublisher;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigLoggingTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private ConfigRequestLogger configRequestLogger;
    private ConfigRequestLoggingAspect aspect;

    @BeforeEach
    void setUp() {
        configRequestLogger = new ConfigRequestLogger(logEventPublisher);
        aspect = new ConfigRequestLoggingAspect(configRequestLogger);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testAspect_Success() throws Throwable {
        Object[] args = {"my-app", "prod", "main"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("Response");

        Object result = aspect.logConfigRequest(joinPoint);
        assertEquals("Response", result);
        
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void testAspect_ExceptionInLogger() throws Throwable {
        Object[] args = {"my-app", "prod", "main"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("Response");
        
        // Mock to throw exception when logging
        ConfigRequestLogger mockLogger = mock(ConfigRequestLogger.class);
        doThrow(new RuntimeException("Simulated exception")).when(mockLogger).logConfigRequest(any(), any(), any());
        
        ConfigRequestLoggingAspect mockAspect = new ConfigRequestLoggingAspect(mockLogger);
        
        Object result = mockAspect.logConfigRequest(joinPoint);
        assertEquals("Response", result);
    }

    @Test
    void testAspect_FewerArgs() throws Throwable {
        Object[] args = {"my-app"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("Response");

        Object result = aspect.logConfigRequest(joinPoint);
        assertEquals("Response", result);
        
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void testLogger_WithRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        configRequestLogger.logConfigRequest("app", "dev", "master");

        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void testLogger_NoRequestContext() {
        configRequestLogger.logConfigRequest("app", null, null);
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void testLogger_ExceptionDuringPublish() {
        doThrow(new RuntimeException("Publish fail")).when(logEventPublisher).publish(any());
        
        assertDoesNotThrow(() -> configRequestLogger.logConfigRequest("app", "dev", "master"));
    }
}
