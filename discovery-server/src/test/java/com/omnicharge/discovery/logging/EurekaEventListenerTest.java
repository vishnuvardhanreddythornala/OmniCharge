package com.omnicharge.discovery.logging;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.eureka.server.event.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EurekaEventListenerTest {

    @Mock private ServiceRegistrationLogger serviceRegistrationLogger;
    @InjectMocks private EurekaEventListener listener;

    @Test
    void handleInstanceRegistered_Success() {
        EurekaInstanceRegisteredEvent event = mock(EurekaInstanceRegisteredEvent.class);
        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(event.getInstanceInfo()).thenReturn(instanceInfo);
        when(instanceInfo.getAppName()).thenReturn("USER-SERVICE");
        when(instanceInfo.getInstanceId()).thenReturn("user-service:8001");
        when(instanceInfo.getStatus()).thenReturn(InstanceInfo.InstanceStatus.UP);

        listener.handleInstanceRegistered(event);

        verify(serviceRegistrationLogger).logServiceRegistration("user-service", "user-service:8001", "UP");
    }

    @Test
    void handleInstanceRegistered_ExceptionHandled() {
        EurekaInstanceRegisteredEvent event = mock(EurekaInstanceRegisteredEvent.class);
        when(event.getInstanceInfo()).thenThrow(new RuntimeException("bad event"));

        assertDoesNotThrow(() -> listener.handleInstanceRegistered(event));
        verifyNoInteractions(serviceRegistrationLogger);
    }

    @Test
    void handleInstanceCanceled_Success() {
        EurekaInstanceCanceledEvent event = mock(EurekaInstanceCanceledEvent.class);
        when(event.getAppName()).thenReturn("PAYMENT-SERVICE");
        when(event.getServerId()).thenReturn("payment:8003");

        listener.handleInstanceCanceled(event);

        verify(serviceRegistrationLogger).logServiceFailure("payment-service", "payment:8003", "Instance cancelled");
    }

    @Test
    void handleInstanceCanceled_ExceptionHandled() {
        EurekaInstanceCanceledEvent event = mock(EurekaInstanceCanceledEvent.class);
        when(event.getAppName()).thenThrow(new RuntimeException("bad"));

        assertDoesNotThrow(() -> listener.handleInstanceCanceled(event));
        verifyNoInteractions(serviceRegistrationLogger);
    }

    @Test
    void handleInstanceRenewed_DoesNothing() {
        EurekaInstanceRenewedEvent event = mock(EurekaInstanceRenewedEvent.class);
        listener.handleInstanceRenewed(event);
        // No interaction expected — intentionally a no-op
        verifyNoInteractions(serviceRegistrationLogger);
    }

    @Test
    void handleRegistryAvailable() {
        EurekaRegistryAvailableEvent event = mock(EurekaRegistryAvailableEvent.class);
        assertDoesNotThrow(() -> listener.handleRegistryAvailable(event));
        verifyNoInteractions(serviceRegistrationLogger);
    }

    @Test
    void handleServerStarted() {
        EurekaServerStartedEvent event = mock(EurekaServerStartedEvent.class);
        assertDoesNotThrow(() -> listener.handleServerStarted(event));
        verifyNoInteractions(serviceRegistrationLogger);
    }
}
