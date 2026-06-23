package com.atma.auth_service.dto;

public record VerifyResetCodeResponse(
        String resetSessionToken,
        String message
) {
}
