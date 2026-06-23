package com.atma.api_gateway.filter;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.atma.api_gateway.client.AuthValidationClient;
import com.atma.api_gateway.dto.ErrorResponse;
import com.atma.api_gateway.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthValidationClient authValidationClient;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            AuthValidationClient authValidationClient,
            ObjectMapper objectMapper
    ) {
        this.jwtUtil = jwtUtil;
        this.authValidationClient = authValidationClient;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();

        if (HttpMethod.OPTIONS.matches(request.getMethod())
                || path.startsWith("/actuator/health")
                || path.startsWith("/actuator/prometheus")
                || path.startsWith("/actuator/info")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/register")
                || path.startsWith("/auth/forgot-password")
                || path.startsWith("/auth/verify-reset-code")
                || path.startsWith("/auth/reset-password")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, request, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            writeUnauthorized(response, request, "Invalid or expired JWT token");
            return;
        }

        AuthValidationClient.ValidationResult validationResult = authValidationClient.validate(token);
        if (validationResult.unavailable()) {
            writeError(response, request, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Unavailable",
                    "Auth token validation service unavailable");
            return;
        }
        if (!validationResult.valid()) {
            writeUnauthorized(response, request, "Invalid or revoked JWT token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        writeError(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", message);
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
