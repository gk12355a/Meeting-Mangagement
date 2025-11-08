package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.DeviceEntity;
import com.cmc.meeting.domain.model.DeviceStatus; // Import enum
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataDeviceRepository extends JpaRepository<DeviceEntity, Long> {

    // THÊM DÒNG NÀY (Spring Data JPA tự hiểu để query)
    List<DeviceEntity> findAllByStatus(DeviceStatus status); 
}