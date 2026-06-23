package com.atma.sensor_service.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.atma.sensor_service.entity.ActuatorState;
import com.atma.sensor_service.entity.DeviceMode;

public record ActuatorStatusResponse(
        Boolean heater,
        Boolean kipas,
        Boolean exhaust,
        DeviceMode mode,
        OffsetDateTime updatedAt
) {
    public static ActuatorStatusResponse from(ActuatorState state) {
        return new ActuatorStatusResponse(
                state.getHeater(),
                state.getKipas(),
                state.getExhaust(),
                state.getMode(),
                toUtc(state.getUpdatedAt())
        );
    }

    private static OffsetDateTime toUtc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
