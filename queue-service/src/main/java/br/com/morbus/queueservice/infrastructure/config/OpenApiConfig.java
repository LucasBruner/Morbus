package br.com.morbus.queueservice.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Morbus — Queue Service",
                version = "v1",
                description = "API de gerenciamento de filas ambulatoriais do SUS"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Token JWT obtido via POST /api/v1/auth/login no auth-service",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Morbus — Queue Service")
                        .description("Projeto desenvolvido durante a fase 5 do curso FIAP")
                        .version("v1")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://github.com/LucasBruner/Morbus")));
    }

    @Bean
    public GroupedOpenApi apiV1() {
        return GroupedOpenApi.builder()
                .group("api-v1")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
