package com.atma.auth_service.dto;

public record ForgotPasswordResponse(
        String message,
        Integer retryAfterSeconds
) {
}
