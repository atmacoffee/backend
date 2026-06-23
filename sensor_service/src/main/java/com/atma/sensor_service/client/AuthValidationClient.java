package com.atma.sensor_service.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthValidationClient {

    private final RestTemplate restTemplate;

    public AuthValidationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ValidationResult validate(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    "http://AUTH-SERVICE/auth/validate",
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    Void.class
            );
            return new ValidationResult(response.getStatusCode().is2xxSuccessful(), false);
        } catch (HttpClientErrorException.Unauthorized ex) {
            return new ValidationResult(false, false);
        } catch (ResourceAccessException ex) {
            return new ValidationResult(false, true);
        }
    }

    public record ValidationResult(boolean valid, boolean unavailable) {
    }
}
