package com.atma.sensor_service.exception;

public class MqttCommandException extends RuntimeException {
    public MqttCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
