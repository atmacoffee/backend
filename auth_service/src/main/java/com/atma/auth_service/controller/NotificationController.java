package com.atma.auth_service.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atma.auth_service.dto.NotificationResponse;
import com.atma.auth_service.dto.UnreadNotificationCountResponse;
import com.atma.auth_service.model.Pengguna;
import com.atma.auth_service.service.AuthService;
import com.atma.auth_service.service.NotificationService;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final AuthService authService;
    private final NotificationService notificationService;

    public NotificationController(AuthService authService, NotificationService notificationService) {
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestHeader("Authorization") String authHeader,
            Pageable pageable
    ) {
        Pengguna pengguna = authService.getCurrentUser(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(notificationService.getNotifications(pengguna, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
            @RequestHeader("Authorization") String authHeader
    ) {
        Pengguna pengguna = authService.getCurrentUser(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(new UnreadNotificationCountResponse(notificationService.getUnreadCount(pengguna)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id
    ) {
        Pengguna pengguna = authService.getCurrentUser(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(notificationService.markRead(pengguna, id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead(@RequestHeader("Authorization") String authHeader) {
        Pengguna pengguna = authService.getCurrentUser(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(Map.of("updatedCount", notificationService.markAllRead(pengguna)));
    }
}
