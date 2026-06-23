package com.atma.sensor_service.dto;

import com.atma.sensor_service.entity.DeviceMode;

import jakarta.validation.constraints.NotNull;

public class DeviceModeRequest {

    @NotNull(message = "mode wajib diisi")
    private DeviceMode mode;

    public DeviceMode getMode() { return mode; }
    public void setMode(DeviceMode mode) { this.mode = mode; }
}
