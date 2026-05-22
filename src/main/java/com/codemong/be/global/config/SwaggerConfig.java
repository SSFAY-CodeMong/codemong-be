package com.codemong.be.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Codemong API",
        description = "Codemong API documentation",
        version = "1"
))
public class SwaggerConfig {

    @Bean
    GroupedOpenApi githubOpenApi() {
        return GroupedOpenApi.builder()
                .group("Github API")
                .pathsToMatch("/github/**")
                .build();
    }
}
