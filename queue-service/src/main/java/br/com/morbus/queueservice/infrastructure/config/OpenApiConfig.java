package br.com.morbus.queueservice.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;

public class OpenApiConfig {
    @Bean
    public OpenAPI LunchTech() {
        return new OpenAPI().info(
                new Info().title("Morbus")
                        .description("Projeto desenvolvido durante a fase 5 do curso FIAP")
                        .version("v0.0.1")
                        .license(new License().name("Apache 2.0").url("https://github.com/LucasBruner/Morbus")));
    }

    @Bean
    public GroupedOpenApi apiV1() {
        return GroupedOpenApi.builder()
                .group("api-v1")
                .pathsToMatch("/v1/**")
                .build();
    }
}
