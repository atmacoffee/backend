package com.atma.sensor_service.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationClient.class);

    private final RestTemplate restTemplate;
    private final String internalServiceToken;

    public NotificationClient(
            RestTemplate restTemplate,
            @Value("${app.internal.service-token}") String internalServiceToken
    ) {
        this.restTemplate = restTemplate;
        this.internalServiceToken = internalServiceToken;
    }

    public void broadcast(String eventKey, String title, String message, String severity) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service-Token", internalServiceToken);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of(
                "eventKey", eventKey,
                "title", title,
                "message", message,
                "severity", severity
        ), headers);
        try {
            restTemplate.postForEntity("http://AUTH-SERVICE/internal/notifications/broadcast", request, Void.class);
        } catch (Exception ex) {
            LOGGER.warn("Failed to broadcast notification {}: {}", eventKey, ex.getMessage());
        }
    }
}
