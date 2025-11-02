package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.device.DeviceDTO;
import com.cmc.meeting.application.dto.device.DeviceRequest;
import com.cmc.meeting.application.port.service.DeviceService;
import com.cmc.meeting.domain.model.Device;
import com.cmc.meeting.domain.port.repository.DeviceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final ModelMapper modelMapper;

    public DeviceServiceImpl(DeviceRepository deviceRepository, ModelMapper modelMapper) {
        this.deviceRepository = deviceRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceDTO> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(device -> modelMapper.map(device, DeviceDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public DeviceDTO createDevice(DeviceRequest request) {
        Device newDevice = modelMapper.map(request, Device.class);
        // newDevice.setStatus(request.getStatus()); // ModelMapper tự làm
        Device savedDevice = deviceRepository.save(newDevice);
        return modelMapper.map(savedDevice, DeviceDTO.class);
    }

    @Override
    public DeviceDTO updateDevice(Long id, DeviceRequest request) {
        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thiết bị: " + id));

        existingDevice.setName(request.getName());
        existingDevice.setDescription(request.getDescription());
        existingDevice.setStatus(request.getStatus());

        Device updatedDevice = deviceRepository.save(existingDevice);
        return modelMapper.map(updatedDevice, DeviceDTO.class);
    }

    @Override
    public void deleteDevice(Long id) {
        if (!deviceRepository.findById(id).isPresent()) {
            throw new EntityNotFoundException("Không tìm thấy thiết bị: " + id);
        }
        deviceRepository.deleteById(id);
    }
}