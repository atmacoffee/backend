package com.atma.sensor_service.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.atma.sensor_service.dto.ActuatorCommandResponse;
import com.atma.sensor_service.dto.ActuatorStatusResponse;
import com.atma.sensor_service.dto.DeviceStatusResponse;
import com.atma.sensor_service.dto.SensorRequest;
import com.atma.sensor_service.dto.SensorResponse;
import com.atma.sensor_service.entity.ActuatorState;
import com.atma.sensor_service.entity.DeviceMode;
import com.atma.sensor_service.entity.Sensor;
import com.atma.sensor_service.exception.MqttCommandException;
import com.atma.sensor_service.repository.ActuatorStateRepository;
import com.atma.sensor_service.repository.SensorRepository;

@Service
public class SensorService {

    private static final Clock UTC_CLOCK = Clock.systemUTC();

    private final SensorRepository sensorRepository;
    private final ActuatorStateRepository actuatorStateRepository;
    private final MqttCommandPublisher mqttCommandPublisher;
    private final NotificationClient notificationClient;
    private final String deviceId;
    private final long offlineThresholdSeconds;
    private final double temperatureIdealMin;
    private final double temperatureIdealMax;
    private final double humidityThreshold;
    private final long eventCooldownSeconds;
    private final Map<String, LocalDateTime> eventCooldowns = new ConcurrentHashMap<>();
    private volatile boolean offlineNotificationActive;

    public SensorService(
            SensorRepository sensorRepository,
            ActuatorStateRepository actuatorStateRepository,
            MqttCommandPublisher mqttCommandPublisher,
            NotificationClient notificationClient,
            @Value("${device.id}") String deviceId,
            @Value("${device.offline-threshold-seconds}") long offlineThresholdSeconds,
            @Value("${notification.temperature-ideal-min}") double temperatureIdealMin,
            @Value("${notification.temperature-ideal-max}") double temperatureIdealMax,
            @Value("${notification.humidity-threshold}") double humidityThreshold,
            @Value("${notification.event-cooldown-seconds}") long eventCooldownSeconds
    ) {
        this.sensorRepository = sensorRepository;
        this.actuatorStateRepository = actuatorStateRepository;
        this.mqttCommandPublisher = mqttCommandPublisher;
        this.notificationClient = notificationClient;
        this.deviceId = deviceId;
        this.offlineThresholdSeconds = offlineThresholdSeconds;
        this.temperatureIdealMin = temperatureIdealMin;
        this.temperatureIdealMax = temperatureIdealMax;
        this.humidityThreshold = humidityThreshold;
        this.eventCooldownSeconds = eventCooldownSeconds;
    }

    public Page<SensorResponse> getAllSensor(Pageable pageable) {
        DeviceMode mode = getOrCreateActuatorState().getMode();
        return sensorRepository.findAll(pageable)
                .map(sensor -> SensorResponse.from(sensor, deviceId, mode));
    }

    public Sensor saveSensor(Sensor sensor) {
        if (sensor.getExhaust() == null) {
            sensor.setExhaust(false);
        }
        return sensorRepository.save(sensor);
    }

    public SensorResponse saveSensor(SensorRequest request) {
        Sensor sensor = new Sensor();
        sensor.setSuhu(request.getSuhu());
        sensor.setKelembaban(request.getKelembaban());
        sensor.setHeater(request.getHeater());
        sensor.setKipas(request.getKipas());
        sensor.setExhaust(Boolean.TRUE.equals(request.getExhaust()));
        Sensor savedSensor = saveSensor(sensor);
        syncActuatorStateFromTelemetry(request);
        evaluateNotificationRules(savedSensor);
        offlineNotificationActive = false;
        return SensorResponse.from(savedSensor, deviceId, getOrCreateActuatorState().getMode());
    }

    public SensorResponse getLatestSensor() {
        return sensorRepository.findTopByOrderByCreatedAtDesc()
                .map(sensor -> SensorResponse.from(sensor, deviceId, getOrCreateActuatorState().getMode()))
                .orElse(SensorResponse.empty(deviceId, getOrCreateActuatorState().getMode()));
    }

    public DeviceStatusResponse getDeviceStatus() {
        Optional<Sensor> latest = sensorRepository.findTopByOrderByCreatedAtDesc();
        if (latest.isEmpty() || latest.get().getCreatedAt() == null) {
            return new DeviceStatusResponse(deviceId, false, null, "Device offline: no sensor data received");
        }

        LocalDateTime lastSeen = latest.get().getCreatedAt();
        long ageSeconds = Duration.between(lastSeen, LocalDateTime.now(UTC_CLOCK)).getSeconds();
        boolean online = ageSeconds <= offlineThresholdSeconds;
        return DeviceStatusResponse.of(
                deviceId,
                online,
                lastSeen,
                online ? "Device online" : "Device offline"
        );
    }

    public ActuatorStatusResponse getActuatorStatus() {
        return ActuatorStatusResponse.from(getOrCreateActuatorState());
    }

    public ActuatorCommandResponse updateActuator(String actuator, boolean enabled) {
        String normalized = actuator.toLowerCase();
        if (!normalized.equals("heater") && !normalized.equals("kipas") && !normalized.equals("exhaust")) {
            throw new IllegalArgumentException("Unsupported actuator: " + actuator);
        }

        try {
            mqttCommandPublisher.publishActuatorCommand(deviceId, normalized, enabled);
        } catch (MqttCommandException ex) {
            maybeBroadcast(
                    "ACTUATOR_COMMAND_FAILED",
                    "Perintah aktuator gagal",
                    actuatorDisplayName(normalized) + " gagal dikirim ke perangkat.",
                    "ERROR"
            );
            throw ex;
        }

        ActuatorState state = getOrCreateActuatorState();
        switch (normalized) {
            case "heater" -> state.setHeater(enabled);
            case "kipas" -> state.setKipas(enabled);
            case "exhaust" -> state.setExhaust(enabled);
            default -> throw new IllegalArgumentException("Unsupported actuator: " + actuator);
        }
        actuatorStateRepository.save(state);

        return new ActuatorCommandResponse(
                normalized,
                enabled,
                "COMMAND_SENT",
                actuatorDisplayName(normalized) + " command sent successfully"
        );
    }

    public ActuatorStatusResponse updateMode(DeviceMode mode) {
        mqttCommandPublisher.publishModeCommand(deviceId, mode);
        ActuatorState state = getOrCreateActuatorState();
        state.setMode(mode);
        return ActuatorStatusResponse.from(actuatorStateRepository.save(state));
    }

    public void evaluateOfflineNotification() {
        Optional<Sensor> latest = sensorRepository.findTopByOrderByCreatedAtDesc();
        if (latest.isEmpty() || latest.get().getCreatedAt() == null) {
            return;
        }
        long ageSeconds = Duration.between(latest.get().getCreatedAt(), LocalDateTime.now(UTC_CLOCK)).getSeconds();
        if (ageSeconds > offlineThresholdSeconds && !offlineNotificationActive) {
            maybeBroadcast(
                    "DEVICE_OFFLINE",
                    "Perangkat offline",
                    "Perangkat " + deviceId + " tidak mengirim telemetry dalam " + offlineThresholdSeconds + " detik terakhir.",
                    "WARNING"
            );
            offlineNotificationActive = true;
        }
    }

    private ActuatorState getOrCreateActuatorState() {
        return actuatorStateRepository.findByDeviceId(deviceId)
                .orElseGet(() -> {
                    ActuatorState state = new ActuatorState();
                    state.setDeviceId(deviceId);
                    return actuatorStateRepository.save(state);
                });
    }

    private String actuatorDisplayName(String actuator) {
        if ("heater".equals(actuator)) {
            return "Heater";
        }
        if ("kipas".equals(actuator)) {
            return "Kipas";
        }
        return "Exhaust";
    }

    private void syncActuatorStateFromTelemetry(SensorRequest request) {
        ActuatorState state = getOrCreateActuatorState();
        state.setHeater(request.getHeater() != null && request.getHeater() == 1);
        state.setKipas(request.getKipas() != null && request.getKipas() == 1);
        state.setExhaust(Boolean.TRUE.equals(request.getExhaust()));
        if (request.getMode() != null) {
            state.setMode(request.getMode());
        }
        actuatorStateRepository.save(state);
    }

    private void evaluateNotificationRules(Sensor sensor) {
        if (sensor.getSuhu() != null && sensor.getSuhu() < temperatureIdealMin) {
            maybeBroadcast(
                    "TEMPERATURE_BELOW_IDEAL",
                    "Suhu di bawah ideal",
                    "Suhu perangkat " + deviceId + " berada di " + sensor.getSuhu()
                            + "°C, di bawah rentang ideal " + temperatureIdealMin + "-" + temperatureIdealMax + "°C.",
                    "WARNING"
            );
        }
        if (sensor.getSuhu() != null && sensor.getSuhu() > temperatureIdealMax) {
            maybeBroadcast(
                    "TEMPERATURE_ABOVE_IDEAL",
                    "Suhu di atas ideal",
                    "Suhu perangkat " + deviceId + " berada di " + sensor.getSuhu()
                            + "°C, melebihi rentang ideal " + temperatureIdealMin + "-" + temperatureIdealMax + "°C.",
                    "WARNING"
            );
        }
        if (sensor.getKelembaban() != null && sensor.getKelembaban() >= humidityThreshold) {
            maybeBroadcast(
                    "HUMIDITY_THRESHOLD_EXCEEDED",
                    "Kelembapan melewati batas",
                    "Kelembapan perangkat " + deviceId + " mencapai " + sensor.getKelembaban()
                            + "%, melebihi batas " + humidityThreshold + "%.",
                    "WARNING"
            );
        }
    }

    private void maybeBroadcast(String eventKey, String title, String message, String severity) {
        LocalDateTime now = LocalDateTime.now(UTC_CLOCK);
        LocalDateTime lastSent = eventCooldowns.get(eventKey);
        if (lastSent != null && Duration.between(lastSent, now).getSeconds() < eventCooldownSeconds) {
            return;
        }
        notificationClient.broadcast(eventKey, title, message, severity);
        eventCooldowns.put(eventKey, now);
    }
}
