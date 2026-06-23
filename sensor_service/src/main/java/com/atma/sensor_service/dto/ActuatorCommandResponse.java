package com.atma.sensor_service.dto;

public record ActuatorCommandResponse(
        String actuator,
        Boolean enabled,
        String status,
        String message
) {
}
