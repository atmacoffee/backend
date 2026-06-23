package com.atma.auth_service.dto;

public record TokenValidationResponse(
        boolean valid,
        String email
) {
}
