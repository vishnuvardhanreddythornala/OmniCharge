package com.omnicharge.recharge.config;

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

    @Value("${server.port:8083}")
    private String serverPort;

    @Bean
    public OpenAPI rechargeServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OmniCharge — Recharge Service API")
                        .description("""
                                Handles mobile recharge initiation, SAGA orchestration, and recharge history.
                                
                                **Features:**
                                - Initiate mobile recharge (triggers Payment + Operator SAGA)
                                - Retrieve recharge by ID
                                - Recharge history with date filtering
                                - Real-time recharge status check
                                - Admin: view all recharges platform-wide
                                """)
                        .version("v1.0.0")
                        .contact(new Contact().name("OmniCharge Platform Team").email("support@omnicharge.com"))
                        .license(new License().name("Private — OmniCharge")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local (Direct)"),
                        new Server().url("http://localhost:8080").description("Local (via API Gateway)"),
                        new Server().url("https://omnicharge.centralindia.cloudapp.azure.com").description("Production (Azure)")
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
