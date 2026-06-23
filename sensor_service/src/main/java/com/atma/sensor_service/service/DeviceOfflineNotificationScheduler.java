package com.atma.sensor_service.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeviceOfflineNotificationScheduler {

    private final SensorService sensorService;

    public DeviceOfflineNotificationScheduler(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @Scheduled(fixedDelayString = "${notification.offline-check-delay-millis}")
    public void checkOfflineStatus() {
        sensorService.evaluateOfflineNotification();
    }
}
