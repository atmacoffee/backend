package com.atma.sensor_service.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.atma.sensor_service.config.MqttBrokerProperties;
import com.atma.sensor_service.entity.DeviceMode;
import com.atma.sensor_service.exception.MqttCommandException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MqttCommandPublisher {

    private static final Clock UTC_CLOCK = Clock.systemUTC();

    private final ObjectMapper objectMapper;
    private final MqttBrokerProperties brokerProperties;
    private final String username;
    private final String password;
    private final String clientId;
    private final String commandTopicPrefix;

    public MqttCommandPublisher(
            ObjectMapper objectMapper,
            MqttBrokerProperties brokerProperties,
            @Value("${mqtt.username:}") String username,
            @Value("${mqtt.password:}") String password,
            @Value("${mqtt.command-client-id}") String clientId,
            @Value("${mqtt.command-topic-prefix}") String commandTopicPrefix
    ) {
        this.objectMapper = objectMapper;
        this.brokerProperties = brokerProperties;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.commandTopicPrefix = commandTopicPrefix;
    }

    public void publishActuatorCommand(String deviceId, String actuator, boolean enabled) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ACTUATOR_COMMAND");
        payload.put("actuator", actuator);
        payload.put("enabled", enabled);
        payload.put("timestamp", LocalDateTime.now(UTC_CLOCK).toString());
        publish(deviceId + "/command/actuator", payload);
    }

    public void publishModeCommand(String deviceId, DeviceMode mode) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "MODE_COMMAND");
        payload.put("mode", mode.name());
        payload.put("timestamp", LocalDateTime.now(UTC_CLOCK).toString());
        publish(deviceId + "/command/mode", payload);
    }

    private void publish(String topicSuffix, Map<String, Object> payload) {
        String topic = commandTopicPrefix + "/" + topicSuffix;
        MqttClient client = null;
        try {
            client = new MqttClient(brokerProperties.brokerUrl(), clientId + "-" + System.nanoTime(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(5);
            options.setAutomaticReconnect(false);
            if (brokerProperties.useTls() && brokerProperties.sslSocketFactory() != null) {
                options.setSocketFactory(brokerProperties.sslSocketFactory());
            }
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
            }
            if (password != null && !password.isBlank()) {
                options.setPassword(password.toCharArray());
            }
            client.connect(options);
            MqttMessage message = new MqttMessage(objectMapper.writeValueAsBytes(payload));
            message.setQos(1);
            client.publish(topic, message);
        } catch (Exception ex) {
            throw new MqttCommandException("Failed to publish MQTT command", ex);
        } finally {
            if (client != null) {
                try {
                    if (client.isConnected()) {
                        client.disconnect();
                    }
                    client.close();
                } catch (Exception ignored) {
                    // No-op. Command publish result is determined by the main try/catch path.
                }
            }
        }
    }
}
