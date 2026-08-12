package com.mybeaufortviewproject.mybeaufortview_backend.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {

        OpenAPI api = new OpenAPI();

        api.setInfo(new Info()
                .title("MyBeaufortView Backend API")
                .version("1.0.0")
                .description("API documentation for MyBeaufortView backend services."));

        Components components = new Components();
        components.addSecuritySchemes(SECURITY_SCHEME_NAME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
        );

        api.setComponents(components);

        api.setSecurity(List.of(new SecurityRequirement()
                .addList(SECURITY_SCHEME_NAME)));

        return api;
    }
}
