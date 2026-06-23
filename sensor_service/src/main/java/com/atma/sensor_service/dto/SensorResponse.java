package com.atma.sensor_service.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.atma.sensor_service.entity.DeviceMode;
import com.atma.sensor_service.entity.Sensor;

public record SensorResponse(
        Long id,
        String deviceId,
        Double temperature,
        Double humidity,
        Boolean heater,
        Boolean kipas,
        Boolean exhaust,
        DeviceMode mode,
        OffsetDateTime createdAt
) {
    public static SensorResponse from(Sensor sensor, String deviceId, DeviceMode mode) {
        return new SensorResponse(
                sensor.getId(),
                deviceId,
                sensor.getSuhu(),
                sensor.getKelembaban(),
                sensor.getHeater() != null && sensor.getHeater() == 1,
                sensor.getKipas() != null && sensor.getKipas() == 1,
                Boolean.TRUE.equals(sensor.getExhaust()),
                mode,
                toUtc(sensor.getCreatedAt())
        );
    }

    public static SensorResponse empty(String deviceId, DeviceMode mode) {
        return new SensorResponse(null, deviceId, null, null, false, false, false, mode, null);
    }

    private static OffsetDateTime toUtc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
