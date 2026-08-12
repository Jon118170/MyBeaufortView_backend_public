package com.mybeaufortviewproject.mybeaufortview_backend.common.config;

import java.util.Map;

import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ActuatorInfoConfig {

    @Bean
    InfoContributor appInfo(Environment env) {
        String activeProfiles = env.getActiveProfiles().length == 0
                ? "default"
                : String.join(",", env.getActiveProfiles());

        return builder -> builder.withDetail("app", Map.of(
                "name", env.getProperty("spring.application.name", "mybeaufortview_backend"),
                "version", env.getProperty("info.app.version", "0.0.0"),
                "profiles", activeProfiles
        ));
    }
}
