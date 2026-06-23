package com.atma.auth_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class SecurityPropertiesValidator {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${eureka.client.service-url.defaultZone:}")
    private String eurekaUrl;

    @PostConstruct
    void validate() {
        if ("prod".equalsIgnoreCase(activeProfile)) {
            if (jwtSecret == null || jwtSecret.length() < 32 || jwtSecret.contains("atma-secret-key")) {
                throw new IllegalStateException("JWT_SECRET must be set to a strong value in prod profile");
            }
            if (datasourceUrl == null || datasourceUrl.isBlank()) {
                throw new IllegalStateException("SPRING_DATASOURCE_URL must be set in prod profile");
            }
            if (datasourceUsername == null || datasourceUsername.isBlank()) {
                throw new IllegalStateException("SPRING_DATASOURCE_USERNAME must be set in prod profile");
            }
            if (datasourcePassword == null || datasourcePassword.isBlank()) {
                throw new IllegalStateException("SPRING_DATASOURCE_PASSWORD must be set in prod profile");
            }
            if (eurekaUrl == null || eurekaUrl.isBlank()) {
                throw new IllegalStateException("EUREKA_SERVER_URL must be set in prod profile");
            }
        }
    }
}
