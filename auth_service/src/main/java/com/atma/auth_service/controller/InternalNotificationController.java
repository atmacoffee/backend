package com.atma.auth_service.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atma.auth_service.dto.BroadcastNotificationRequest;
import com.atma.auth_service.exception.ForbiddenException;
import com.atma.auth_service.service.NotificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private final NotificationService notificationService;
    private final String internalServiceToken;

    public InternalNotificationController(
            NotificationService notificationService,
            @Value("${app.internal.service-token}") String internalServiceToken
    ) {
        this.notificationService = notificationService;
        this.internalServiceToken = internalServiceToken;
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, String>> broadcast(
            @RequestHeader("X-Internal-Service-Token") String headerToken,
            @Valid @RequestBody BroadcastNotificationRequest request
    ) {
        if (!internalServiceToken.equals(headerToken)) {
            throw new ForbiddenException("Internal service token tidak valid");
        }
        notificationService.broadcast(request);
        return ResponseEntity.accepted().body(Map.of("message", "Notification broadcast accepted"));
    }
}
