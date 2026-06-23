package com.atma.sensor_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atma.sensor_service.entity.ActuatorState;

public interface ActuatorStateRepository extends JpaRepository<ActuatorState, Long> {
    Optional<ActuatorState> findByDeviceId(String deviceId);
}
