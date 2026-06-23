package com.atma.sensor_service.entity;

import java.time.Clock;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "actuator_state")
public class ActuatorState {

    private static final Clock UTC_CLOCK = Clock.systemUTC();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true)
    private String deviceId;

    @Column(nullable = false)
    private Boolean heater = false;

    @Column(nullable = false)
    private Boolean kipas = false;

    @Column(nullable = false)
    private Boolean exhaust = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceMode mode = DeviceMode.MANUAL;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now(UTC_CLOCK);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Boolean getHeater() { return heater; }
    public void setHeater(Boolean heater) { this.heater = heater; }
    public Boolean getKipas() { return kipas; }
    public void setKipas(Boolean kipas) { this.kipas = kipas; }
    public Boolean getExhaust() { return exhaust; }
    public void setExhaust(Boolean exhaust) { this.exhaust = exhaust; }
    public DeviceMode getMode() { return mode; }
    public void setMode(DeviceMode mode) { this.mode = mode; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
