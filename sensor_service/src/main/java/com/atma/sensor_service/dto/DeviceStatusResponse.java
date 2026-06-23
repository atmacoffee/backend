package com.atma.sensor_service.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record DeviceStatusResponse(
        String deviceId,
        boolean online,
        OffsetDateTime lastSeen,
        String message
) {
    public static DeviceStatusResponse of(
            String deviceId,
            boolean online,
            LocalDateTime lastSeen,
            String message
    ) {
        return new DeviceStatusResponse(
                deviceId,
                online,
                lastSeen == null ? null : lastSeen.atOffset(ZoneOffset.UTC),
                message
        );
    }
}
