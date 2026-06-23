package com.atma.sensor_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atma.sensor_service.dto.ActuatorCommandRequest;
import com.atma.sensor_service.dto.ActuatorCommandResponse;
import com.atma.sensor_service.dto.ActuatorStatusResponse;
import com.atma.sensor_service.dto.DeviceModeRequest;
import com.atma.sensor_service.dto.DeviceStatusResponse;
import com.atma.sensor_service.dto.SensorRequest;
import com.atma.sensor_service.dto.SensorResponse;
import com.atma.sensor_service.service.SensorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/sensor")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping
    public ResponseEntity<Page<SensorResponse>> getAllSensor(Pageable pageable) {
        return ResponseEntity.ok(sensorService.getAllSensor(pageable));
    }

    @GetMapping("/latest")
    public ResponseEntity<SensorResponse> getLatestSensor() {
        return ResponseEntity.ok(sensorService.getLatestSensor());
    }

    @GetMapping("/device/status")
    public ResponseEntity<DeviceStatusResponse> getDeviceStatus() {
        return ResponseEntity.ok(sensorService.getDeviceStatus());
    }

    @PostMapping("/device/mode")
    public ResponseEntity<ActuatorStatusResponse> updateMode(@Valid @RequestBody DeviceModeRequest request) {
        return ResponseEntity.ok(sensorService.updateMode(request.getMode()));
    }

    @GetMapping("/actuator/status")
    public ResponseEntity<ActuatorStatusResponse> getActuatorStatus() {
        return ResponseEntity.ok(sensorService.getActuatorStatus());
    }

    @PostMapping("/actuator/{actuator}")
    public ResponseEntity<ActuatorCommandResponse> updateActuator(
            @PathVariable String actuator,
            @Valid @RequestBody ActuatorCommandRequest request
    ) {
        return ResponseEntity.ok(sensorService.updateActuator(actuator, request.getEnabled()));
    }

    @PostMapping
    public ResponseEntity<SensorResponse> saveSensor(@Valid @RequestBody SensorRequest sensor) {
        return ResponseEntity.ok(sensorService.saveSensor(sensor));
    }
}
