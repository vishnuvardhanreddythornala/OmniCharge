package com.omnicharge.common.logging;

/*
 * Centralized constants for the logging infrastructure.
 * Shared by all services (producers) and the logging-service (consumer).
 */
public final class LoggingConstants {

    private LoggingConstants() {
        // Utility class — no instantiation
    }

    /* Dedicated exchange for log events (separate from business exchanges) */
    public static final String LOGGING_EXCHANGE = "omnicharge.logging.exchange";

    /*Queue consumed by logging-service */
    public static final String LOGGING_QUEUE = "logging.events.queue";

    /*Routing key pattern: log.{serviceName} */
    public static final String LOGGING_ROUTING_KEY = "log.#";
}
