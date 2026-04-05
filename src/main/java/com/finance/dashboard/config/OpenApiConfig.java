package com.finance.dashboard.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;


@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "Finance Dashboard API",
                version     = "1.0.0",
                description = "Backend API for a role-based finance dashboard system. " +
                              "Supports user management, financial records CRUD, and dashboard analytics.",
                contact     = @Contact(name = "Finance Dashboard Team")
        ),
        servers = { @Server(url = "/api", description = "Default server") },
        security = { @SecurityRequirement(name = "bearerAuth") }
)
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        in          = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
  
}
