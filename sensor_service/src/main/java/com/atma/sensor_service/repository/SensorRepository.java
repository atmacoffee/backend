package com.atma.sensor_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.atma.sensor_service.entity.Sensor;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
    java.util.Optional<Sensor> findTopByOrderByCreatedAtDesc();
}
