package com.omnicharge.operator.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RabbitMQEventLoggerTest {
    @Mock private LogEventPublisher logEventPublisher;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private RabbitListener rabbitListener;
    @Mock private MethodSignature signature;
    @InjectMocks private RabbitMQEventLogger logger;

    @Test
    void testLogConsumption() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(this.getClass().getDeclaredMethods()[0]);
        when(signature.getDeclaringType()).thenReturn(this.getClass());
        when(rabbitListener.queues()).thenReturn(new String[]{"testQ"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"msg"});
        when(joinPoint.proceed()).thenReturn(null);

        logger.logConsumption(joinPoint, rabbitListener);
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void testLogPublishing() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"exchange", "routingKey", "msg"});
        when(joinPoint.proceed()).thenReturn(null);
        logger.logPublishing(joinPoint);
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }
}
