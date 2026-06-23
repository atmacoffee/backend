package com.atma.sensor_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class SecurityPropertiesValidator {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Value("${mqtt.username:}")
    private String mqttUsername;

    @Value("${mqtt.password:}")
    private String mqttPassword;

    @Value("${device.id}")
    private String deviceId;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${eureka.client.service-url.defaultZone:}")
    private String eurekaUrl;

    @Value("${mqtt.topic:}")
    private String mqttTopic;

    @Value("${mqtt.command-topic-prefix:}")
    private String mqttCommandTopicPrefix;

    private final MqttBrokerProperties brokerProperties;

    public SecurityPropertiesValidator(MqttBrokerProperties brokerProperties) {
        this.brokerProperties = brokerProperties;
    }

    @PostConstruct
    void validate() {
        if ("prod".equalsIgnoreCase(activeProfile)) {
            if (deviceId == null || deviceId.isBlank()) {
                throw new IllegalStateException("DEVICE_ID must be configured in prod profile");
            }
            if (datasourceUrl == null || datasourceUrl.isBlank()) {
                throw new IllegalStateException("SPRING_DATASOURCE_URL must be set in prod profile");
            }
            if (datasourceUsername == null || datasourceUsername.isBlank()) {
                throw new IllegalStateException("SPRING_DATASOURCE_USERNAME must be set in prod profile");
            }
            if (datasourcePassword == null || datasourcePassword.isBlank()) {
                throw new IllegalStateException("SPRING_DATASOURCE_PASSWORD must be set in prod profile");
            }
            if (brokerProperties.brokerUrl() == null || brokerProperties.brokerUrl().isBlank()) {
                throw new IllegalStateException("MQTT broker URL must be configured in prod profile");
            }
            if (mqttUsername == null || mqttUsername.isBlank() || mqttPassword == null || mqttPassword.isBlank()) {
                throw new IllegalStateException("MQTT username and password must be configured in prod profile");
            }
            if (eurekaUrl == null || eurekaUrl.isBlank()) {
                throw new IllegalStateException("EUREKA_SERVER_URL must be set in prod profile");
            }
            if (mqttTopic == null || mqttTopic.isBlank()) {
                throw new IllegalStateException("MQTT_SENSOR_TOPIC must be set in prod profile");
            }
            if (mqttCommandTopicPrefix == null || mqttCommandTopicPrefix.isBlank()) {
                throw new IllegalStateException("MQTT_COMMAND_TOPIC_PREFIX must be set in prod profile");
            }
        }
    }
}
