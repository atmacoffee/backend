package com.atma.auth_service.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.atma.auth_service.model.Notification;

public record NotificationResponse(
        Long id,
        String eventKey,
        String title,
        String message,
        String severity,
        boolean read,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventKey(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getSeverity(),
                notification.isRead(),
                toUtc(notification.getCreatedAt()),
                toUtc(notification.getReadAt())
        );
    }

    private static OffsetDateTime toUtc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
