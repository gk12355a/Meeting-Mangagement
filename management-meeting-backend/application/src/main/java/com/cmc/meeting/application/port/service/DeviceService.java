package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.device.DeviceDTO;
import com.cmc.meeting.application.dto.device.DeviceRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface DeviceService {
    List<DeviceDTO> getAllDevices();
    DeviceDTO createDevice(DeviceRequest request);
    DeviceDTO updateDevice(Long id, DeviceRequest request);
    void deleteDevice(Long id);
    List<DeviceDTO> findAvailableDevices(LocalDateTime startTime, LocalDateTime endTime);
}