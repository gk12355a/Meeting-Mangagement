package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDeviceRepository extends JpaRepository<DeviceEntity, Long> {
}