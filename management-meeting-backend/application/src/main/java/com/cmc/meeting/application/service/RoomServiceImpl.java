package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.room.RoomDTO;
import com.cmc.meeting.application.dto.room.RoomRequest;
import com.cmc.meeting.application.port.service.RoomService;
import com.cmc.meeting.domain.model.Room;
import com.cmc.meeting.domain.port.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final com.cmc.meeting.application.port.storage.FileStoragePort fileStoragePort;
    private final com.cmc.meeting.domain.port.repository.DeviceRepository deviceRepository; // Injected

    public RoomServiceImpl(RoomRepository roomRepository, ModelMapper modelMapper,
            com.cmc.meeting.application.port.storage.FileStoragePort fileStoragePort,
            com.cmc.meeting.domain.port.repository.DeviceRepository deviceRepository) {
        this.roomRepository = roomRepository;
        this.modelMapper = modelMapper;
        this.fileStoragePort = fileStoragePort;
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(room -> modelMapper.map(room, RoomDTO.class))
                .collect(Collectors.toList());
    }

    // === MIGRATION LOGIC (US-33) ===
    @jakarta.annotation.PostConstruct
    public void migrateLocationData() {
        // ... (Keep existing migration logic if needed, or remove if obsolete. Keeping
        // for safety)
        List<Room> rooms = roomRepository.findAll().stream()
                .map(entity -> modelMapper.map(entity, Room.class))
                .collect(Collectors.toList());

        for (Room room : rooms) {
            boolean changed = false;
            if (room.getFloor() == null && room.getLocation() != null && !room.getLocation().isEmpty()) {
                parseLocationToBuildingAndFloor(room);
                changed = true;
            } else if ((room.getLocation() == null || room.getLocation().isEmpty())
                    && room.getFloor() != null) {
                room.setLocation(buildLocationString(room.getBuildingName(), room.getFloor()));
                changed = true;
            }

            if (changed) {
                roomRepository.save(room);
            }
        }
    }

    private void parseLocationToBuildingAndFloor(Room room) {
        String loc = room.getLocation();
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("Tầng\\s+(\\d+)(.*)",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(loc);

            if (m.find()) {
                String floorStr = m.group(1);
                String rest = m.group(2).trim();
                room.setFloor(Integer.parseInt(floorStr));
                if (!rest.isEmpty()) {
                    String building = rest.replaceAll("^[-–,.]+\\s*", "").trim();
                    room.setBuildingName(building);
                }
            }
        } catch (Exception e) {
            System.err.println("Không thể parse location: " + loc);
        }
    }

    private String buildLocationString(String building, Integer floor) {
        if (floor == null)
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Tầng ").append(floor);
        if (building != null && !building.isEmpty()) {
            sb.append(" - ").append(building);
        }
        return sb.toString();
    }

    @Override
    public RoomDTO createRoom(RoomRequest request, List<org.springframework.web.multipart.MultipartFile> images) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên phòng không được để trống");
        }
        if (request.getCapacity() == null || request.getCapacity() < 1) {
            throw new IllegalArgumentException("Sức chứa phải ít nhất là 1");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        Room newRoom = modelMapper.map(request, Room.class);

        if (newRoom.getFloor() != null) {
            newRoom.setLocation(buildLocationString(newRoom.getBuildingName(), newRoom.getFloor()));
        } else if (newRoom.getLocation() != null) {
            parseLocationToBuildingAndFloor(newRoom);
        }

        // Handle images
        if (images != null && !images.isEmpty()) {
            java.util.List<String> imageUrls = new java.util.ArrayList<>();
            for (org.springframework.web.multipart.MultipartFile image : images) {
                if (!image.isEmpty()) {
                    try {
                        java.util.Map<String, String> result = fileStoragePort.uploadFile(image);
                        imageUrls.add(result.get("url"));
                    } catch (java.io.IOException e) {
                        throw new RuntimeException("Upload failed: " + e.getMessage());
                    }
                }
            }
            newRoom.setImages(imageUrls);
        }

        Room savedRoom = roomRepository.save(newRoom);

        // Handle Devices
        if (request.getDeviceIds() != null && !request.getDeviceIds().isEmpty()) {
            for (Long deviceId : request.getDeviceIds()) {
                deviceRepository.findById(deviceId).ifPresent(device -> {
                    device.setRoomId(savedRoom.getId());
                    deviceRepository.save(device);
                });
            }
        }

        return modelMapper.map(savedRoom, RoomDTO.class);
    }

    @Override
    public RoomDTO updateRoom(Long id, RoomRequest request,
            List<org.springframework.web.multipart.MultipartFile> images) {
        Room existingRoom = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng: " + id));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            existingRoom.setName(request.getName());
        }
        if (request.getCapacity() != null && request.getCapacity() > 0) {
            existingRoom.setCapacity(request.getCapacity());
        }
        // Handle Devices Update
        if (request.getDeviceIds() != null) {
            // 1. Unlink devices not in the new list
            List<com.cmc.meeting.domain.model.Device> currentDevices = deviceRepository.findAllByRoomId(id);
            for (com.cmc.meeting.domain.model.Device device : currentDevices) {
                if (!request.getDeviceIds().contains(device.getId())) {
                    device.setRoomId(null);
                    deviceRepository.save(device);
                }
            }
            // 2. Link devices in the new list
            for (Long deviceId : request.getDeviceIds()) {
                deviceRepository.findById(deviceId).ifPresent(device -> {
                    if (!id.equals(device.getRoomId())) { // Only save if changed
                        device.setRoomId(id);
                        deviceRepository.save(device);
                    }
                });
            }
        }

        if (request.getRequiredRoles() != null) {
            existingRoom.setRequiredRoles(request.getRequiredRoles());
        }
        if (request.getStatus() != null) {
            existingRoom.setStatus(request.getStatus());
        }
        if (request.getRequiresApproval() != null) {
            existingRoom.setRequiresApproval(request.getRequiresApproval());
        }

        if (request.getFloor() != null) {
            existingRoom.setFloor(request.getFloor());
            existingRoom.setBuildingName(request.getBuildingName());
            existingRoom.setLocation(buildLocationString(request.getBuildingName(), request.getFloor()));
        }
        if (request.getLocation() != null && !request.getLocation().equals(existingRoom.getLocation())) {
            existingRoom.setLocation(request.getLocation());
            parseLocationToBuildingAndFloor(existingRoom);
        }

        if (request.getDeleteImages() != null && !request.getDeleteImages().isEmpty()) {
            if (existingRoom.getImages() != null) {
                existingRoom.getImages().removeAll(request.getDeleteImages());
            }
        }

        if (images != null && !images.isEmpty()) {
            if (existingRoom.getImages() == null) {
                existingRoom.setImages(new java.util.ArrayList<>());
            }
            for (org.springframework.web.multipart.MultipartFile image : images) {
                if (!image.isEmpty()) {
                    try {
                        java.util.Map<String, String> result = fileStoragePort.uploadFile(image);
                        existingRoom.getImages().add(result.get("url"));
                    } catch (java.io.IOException e) {
                        throw new RuntimeException("Upload failed: " + e.getMessage());
                    }
                }
            }
        }

        Room updatedRoom = roomRepository.save(existingRoom);
        return modelMapper.map(updatedRoom, RoomDTO.class);
    }

    // ... keep deleteRoom and findAvailableRooms
    @Override
    public void deleteRoom(Long id) {
        if (!roomRepository.findById(id).isPresent()) {
            throw new EntityNotFoundException("Không tìm thấy phòng: " + id);
        }
        roomRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> findAvailableRooms(LocalDateTime startTime, LocalDateTime endTime, int capacity) {
        List<Room> availableRooms = roomRepository.findAvailableRooms(startTime, endTime, capacity);
        return availableRooms.stream()
                .map(room -> modelMapper.map(room, RoomDTO.class))
                .collect(Collectors.toList());
    }
}