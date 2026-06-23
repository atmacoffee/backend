package com.atma.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class SecurityPropertiesValidator {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${eureka.client.service-url.defaultZone:}")
    private String eurekaUrl;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @PostConstruct
    void validate() {
        if ("prod".equalsIgnoreCase(activeProfile)) {
            if (jwtSecret == null || jwtSecret.length() < 32 || jwtSecret.contains("atma-secret-key")) {
                throw new IllegalStateException("JWT_SECRET must be set to a strong value in prod profile");
            }
            if (eurekaUrl == null || eurekaUrl.isBlank()) {
                throw new IllegalStateException("EUREKA_SERVER_URL must be set in prod profile");
            }
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                throw new IllegalStateException("CORS_ALLOWED_ORIGINS must be set in prod profile");
            }
        }
    }
}
