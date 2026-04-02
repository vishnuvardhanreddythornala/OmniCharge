package com.omnicharge.logging.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Logging & Monitoring Service API",
        version = "v1.0.0",
        description = "Centralized log querying, trace analysis, and monitoring endpoints."
    )
)
public class OpenApiConfig {
}
