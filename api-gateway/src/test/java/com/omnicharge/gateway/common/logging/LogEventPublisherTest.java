package com.omnicharge.gateway.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogEventPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private FallbackLogWriter fallbackLogWriter;
    @InjectMocks private LogEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "serviceName", "api-gateway");
    }

    @Test
    void publish_Success() {
        LogEvent event = new LogEvent();
        event.setServiceName("api-gateway");
        event.setLevel("INFO");
        event.setMessage("test");

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(
                LoggingConstants.LOGGING_EXCHANGE,
                "log.api-gateway",
                (Object) event
        );
    }

    @Test
    void publish_RabbitDown_FallbackCalled() {
        LogEvent event = new LogEvent();
        event.setServiceName("api-gateway");
        event.setLevel("ERROR");
        event.setMessage("fail");

        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(LogEvent.class));

        publisher.publish(event);

        verify(fallbackLogWriter).writeToFallbackFile(event);
    }
}
