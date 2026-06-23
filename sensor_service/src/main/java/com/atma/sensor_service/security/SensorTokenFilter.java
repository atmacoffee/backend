package com.atma.sensor_service.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.atma.sensor_service.client.AuthValidationClient;
import com.atma.sensor_service.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SensorTokenFilter extends OncePerRequestFilter {

    private final AuthValidationClient authValidationClient;
    private final ObjectMapper objectMapper;

    public SensorTokenFilter(AuthValidationClient authValidationClient, ObjectMapper objectMapper) {
        this.authValidationClient = authValidationClient;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (HttpMethod.OPTIONS.matches(request.getMethod())
                || path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || path.startsWith("/actuator/prometheus")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "Missing or invalid Authorization header");
            return;
        }

        AuthValidationClient.ValidationResult validationResult =
                authValidationClient.validate(authHeader.substring(7));
        if (validationResult.unavailable()) {
            writeError(response, request, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Unavailable",
                    "Auth token validation service unavailable");
            return;
        }
        if (!validationResult.valid()) {
            writeError(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "Invalid or revoked JWT token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(
            HttpServletResponse response,
            HttpServletRequest request,
            int status,
            String error,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                LocalDateTime.now(),
                status,
                error,
                message,
                request.getRequestURI()
        ));
    }
}
