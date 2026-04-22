package com.omnicharge.recharge.common.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class LogEventPublisherTest {
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private FallbackLogWriter fallbackLogWriter;
    @InjectMocks private LogEventPublisher publisher;

    @Test
    void testPublish() {
        LogEvent event = LogEvent.builder().eventType("TEST").build();
        publisher.publish(event);
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(anyString(), anyString(), eq(event));
    }
    
    @Test
    void testPublishFail() {
        LogEvent event = LogEvent.builder().eventType("TEST").build();
        doThrow(new RuntimeException()).when(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(event));
        publisher.publish(event);
        verify(fallbackLogWriter, atLeastOnce()).writeToFallbackFile(eq(event));
    }
}
