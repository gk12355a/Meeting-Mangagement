package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.device.DeviceDTO;
import com.cmc.meeting.application.dto.device.DeviceRequest;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface DeviceService {
    List<DeviceDTO> getAllDevices();

    DeviceDTO createDevice(DeviceRequest request, List<MultipartFile> images);

    DeviceDTO updateDevice(Long id, DeviceRequest request, List<MultipartFile> images);

    void deleteDevice(Long id);

    List<DeviceDTO> findAvailableDevices(LocalDateTime startTime, LocalDateTime endTime);
}