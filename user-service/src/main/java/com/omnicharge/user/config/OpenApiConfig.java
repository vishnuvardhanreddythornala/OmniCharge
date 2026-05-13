package com.omnicharge.user.config;

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

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OmniCharge — User Service API")
                        .description("""
                                Handles all authentication, user profile, OTP verification, and admin user management.
                                
                                **Features:**
                                - Email/Password login with 2FA
                                - Google OAuth2 login
                                - Mobile OTP authentication
                                - Email OTP authentication
                                - JWT token refresh
                                - User profile management
                                - Admin: list, suspend, activate users
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("OmniCharge Platform Team")
                                .email("support@omnicharge.com"))
                        .license(new License().name("Private — OmniCharge")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local (Direct)"),
                        new Server().url("http://localhost:8080").description("Local (via API Gateway)"),
                        new Server().url("https://omnicharge.centralindia.cloudapp.azure.com").description("Production (Azure)")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token obtained from /api/auth/login")));
    }
}
