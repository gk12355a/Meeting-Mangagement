package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.device.DeviceDTO;
import com.cmc.meeting.application.dto.device.DeviceRequest;
import com.cmc.meeting.application.port.service.DeviceService;
import com.cmc.meeting.domain.model.Device;
import com.cmc.meeting.domain.model.DeviceStatus;
import com.cmc.meeting.domain.port.repository.DeviceRepository;
import com.cmc.meeting.domain.port.repository.MeetingRepository;

import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final ModelMapper modelMapper;
    private final MeetingRepository meetingRepository;

    public DeviceServiceImpl(DeviceRepository deviceRepository, ModelMapper modelMapper, MeetingRepository meetingRepository) {
        this.deviceRepository = deviceRepository;
        this.modelMapper = modelMapper;
        this.meetingRepository = meetingRepository;
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
    @Override
    @Transactional(readOnly = true) // Thêm readOnly cho hàm tìm kiếm
    public List<DeviceDTO> findAvailableDevices(LocalDateTime startTime, LocalDateTime endTime) {
        
        // Bước 1: Lấy ID của tất cả các thiết bị đã BỊ ĐẶT trong khung giờ này.
        // LỖI 1 ĐÃ SỬA: meetingRepository giờ đã được nhận diện
        Set<Long> bookedDeviceIds = meetingRepository.findBookedDevicesInTimeRange(startTime, endTime);

        // Bước 2: Lấy tất cả thiết bị CÓ THỂ SỬ DỤNG (trạng thái "AVAILABLE")
        // LỖI 2 SẼ SỬA Ở DƯỚI:
        List<Device> allAvailableDevices = deviceRepository.findAllByStatus(DeviceStatus.AVAILABLE);

        // Bước 3: Lọc ra danh sách cuối cùng
        List<Device> trulyAvailableDevices = allAvailableDevices.stream()
            .filter(device -> !bookedDeviceIds.contains(device.getId()))
            .collect(Collectors.toList());

        // Bước 4: Map sang DTO để trả về
        return trulyAvailableDevices.stream()
                .map(device -> modelMapper.map(device, DeviceDTO.class))
                .collect(Collectors.toList());
    }
}