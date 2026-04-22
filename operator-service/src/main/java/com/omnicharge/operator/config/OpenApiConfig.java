package com.omnicharge.operator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8082}")
    private String serverPort;

    @Bean
    public OpenAPI operatorServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OmniCharge — Operator Service API")
                        .description("""
                                Manages telecom operators, recharge plans, and mobile number operator detection.
                                
                                **Features:**
                                - CRUD for operators and plans (admin-only)
                                - Mobile number operator detection via external API
                                - User-facing plan search (from Redis cache)
                                - Plan activate/deactivate lifecycle management
                                """)
                        .version("v1.0.0")
                        .contact(new Contact().name("OmniCharge Platform Team").email("support@omnicharge.com"))
                        .license(new License().name("Private — OmniCharge")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local (Direct)"),
                        new Server().url("http://localhost:8080").description("Local (via API Gateway)"),
                        new Server().url("https://api.omnicharge.com").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token from /api/auth/login")));
    }
}
