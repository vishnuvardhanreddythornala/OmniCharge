package com.omnicharge.discovery.logging;

import com.netflix.appinfo.InstanceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.eureka.server.event.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to Eureka server events and logs them via ServiceRegistrationLogger.
 * 
 * Production-grade event logging:
 * - Captures service registrations, cancellations, and renewals
 * - Non-blocking, fail-safe
 * - Minimal performance impact
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EurekaEventListener {

    private final ServiceRegistrationLogger serviceRegistrationLogger;

    /**
     * Logs when a service instance registers with Eureka.
     */
    @EventListener
    public void handleInstanceRegistered(EurekaInstanceRegisteredEvent event) {
        try {
            InstanceInfo instanceInfo = event.getInstanceInfo();
            String serviceName = instanceInfo.getAppName().toLowerCase();
            String instanceId = instanceInfo.getInstanceId();
            String status = instanceInfo.getStatus().name();
            
            serviceRegistrationLogger.logServiceRegistration(serviceName, instanceId, status);
        } catch (Exception e) {
            log.debug("Error handling instance registered event: {}", e.getMessage());
        }
    }

    /**
     * Logs when a service instance is cancelled (deregistered).
     */
    @EventListener
    public void handleInstanceCanceled(EurekaInstanceCanceledEvent event) {
        try {
            String serviceName = event.getAppName().toLowerCase();
            String instanceId = event.getServerId();
            
            serviceRegistrationLogger.logServiceFailure(serviceName, instanceId, "Instance cancelled");
        } catch (Exception e) {
            log.debug("Error handling instance canceled event: {}", e.getMessage());
        }
    }

    /**
     * Logs when a service instance renews its lease (heartbeat).
     * Only logs failures, not every successful renewal (too noisy).
     */
    @EventListener
    public void handleInstanceRenewed(EurekaInstanceRenewedEvent event) {
        // Intentionally not logging successful renewals - too verbose
        // Heartbeat failures are logged by Eureka's eviction mechanism
    }

    /**
     * Logs when Eureka server starts up.
     */
    @EventListener
    public void handleRegistryAvailable(EurekaRegistryAvailableEvent event) {
        log.info("[EUREKA-REGISTRY] Registry is now available for service registrations");
    }

    /**
     * Logs when Eureka server starts up.
     */
    @EventListener
    public void handleServerStarted(EurekaServerStartedEvent event) {
        log.info("[EUREKA-SERVER] Eureka server has started successfully");
    }
}
