package com.atma.sensor_service.dto;

import jakarta.validation.constraints.NotNull;

public class ActuatorCommandRequest {

    @NotNull(message = "enabled wajib diisi")
    private Boolean enabled;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
