package com.mybeaufortviewproject.mybeaufortview_backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
public class PostgresFlywaySmokeTest {

    @Container
    public static PostgreSQLContainer postgres = createPostgresContainer();

    @SuppressWarnings("resource")
    private static PostgreSQLContainer createPostgresContainer() {
        return new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
    }

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry) {

        // Force Spring to use Postgres instead of H2 for Flyway migrations
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Enable production-like behavior
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type", () -> "TIMESTAMP");

        // Prevent unrelated infra from interfering with this test
        registry.add("aws.s3.enabled", () -> "false");
    }

    @Test
    public void contextLoads_onRealPostgres_withFlyway() {
        // Test passes if context loads successfully with Flyway migrations
    }

}
