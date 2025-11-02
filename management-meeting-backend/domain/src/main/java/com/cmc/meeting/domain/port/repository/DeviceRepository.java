package com.cmc.meeting.domain.port.repository;

import com.cmc.meeting.domain.model.Device;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository {
    List<Device> findAll();
    Optional<Device> findById(Long id);
    Device save(Device device);
    void deleteById(Long id);
}