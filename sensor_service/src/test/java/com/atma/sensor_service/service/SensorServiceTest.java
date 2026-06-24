package com.atma.sensor_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atma.sensor_service.dto.SensorRequest;
import com.atma.sensor_service.entity.ActuatorState;
import com.atma.sensor_service.entity.DeviceMode;
import com.atma.sensor_service.entity.Sensor;
import com.atma.sensor_service.repository.ActuatorStateRepository;
import com.atma.sensor_service.repository.SensorRepository;

class SensorServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private ActuatorStateRepository actuatorStateRepository;

    @Mock
    private MqttCommandPublisher mqttCommandPublisher;

    @Mock
    private NotificationClient notificationClient;

    private SensorService sensorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sensorService = new SensorService(
                sensorRepository,
                actuatorStateRepository,
                mqttCommandPublisher,
                notificationClient,
                "atma-dryer-001",
                60,
                50.0,
                55.0,
                70.0,
                300
        );
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void telemetryModeUpdatesActuatorState() {
        ActuatorState state = existingState(DeviceMode.MANUAL);
        when(actuatorStateRepository.findByDeviceId("atma-dryer-001")).thenReturn(Optional.of(state));
        when(actuatorStateRepository.save(any(ActuatorState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SensorRequest request = telemetry();
        request.setMode(DeviceMode.AUTO);

        sensorService.saveSensor(request);

        ArgumentCaptor<ActuatorState> captor = ArgumentCaptor.forClass(ActuatorState.class);
        verify(actuatorStateRepository).save(captor.capture());
        assertEquals(DeviceMode.AUTO, captor.getValue().getMode());
    }

    @Test
    void legacyTelemetryWithoutModeKeepsPreviousMode() {
        ActuatorState state = existingState(DeviceMode.AUTO);
        when(actuatorStateRepository.findByDeviceId("atma-dryer-001")).thenReturn(Optional.of(state));
        when(actuatorStateRepository.save(any(ActuatorState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sensorService.saveSensor(telemetry());

        ArgumentCaptor<ActuatorState> captor = ArgumentCaptor.forClass(ActuatorState.class);
        verify(actuatorStateRepository).save(captor.capture());
        assertEquals(DeviceMode.AUTO, captor.getValue().getMode());
    }

    private SensorRequest telemetry() {
        SensorRequest request = new SensorRequest();
        request.setSuhu(52.0);
        request.setKelembaban(60.0);
        request.setHeater(1);
        request.setKipas(1);
        request.setExhaust(false);
        return request;
    }

    private ActuatorState existingState(DeviceMode mode) {
        ActuatorState state = new ActuatorState();
        state.setDeviceId("atma-dryer-001");
        state.setMode(mode);
        return state;
    }
}
