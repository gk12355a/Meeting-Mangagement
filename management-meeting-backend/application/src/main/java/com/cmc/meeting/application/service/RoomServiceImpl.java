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

    public RoomServiceImpl(RoomRepository roomRepository, ModelMapper modelMapper,
            com.cmc.meeting.application.port.storage.FileStoragePort fileStoragePort) {
        this.roomRepository = roomRepository;
        this.modelMapper = modelMapper;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(room -> modelMapper.map(room, RoomDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomDTO createRoom(RoomRequest request, List<org.springframework.web.multipart.MultipartFile> images) {
        Room newRoom = modelMapper.map(request, Room.class);

        // Xử lý upload ảnh
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
        return modelMapper.map(savedRoom, RoomDTO.class);
    }

    @Override
    public RoomDTO updateRoom(Long id, RoomRequest request,
            List<org.springframework.web.multipart.MultipartFile> images) {
        Room existingRoom = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng: " + id));

        // Cập nhật các trường
        existingRoom.setName(request.getName());
        existingRoom.setCapacity(request.getCapacity());
        existingRoom.setLocation(request.getLocation());
        existingRoom.setFixedDevices(request.getFixedDevices());
        existingRoom.setRequiredRoles(request.getRequiredRoles());
        existingRoom.setStatus(request.getStatus());
        if (request.getRequiresApproval() != null) {
            existingRoom.setRequiresApproval(request.getRequiresApproval());
        }

        // Xử lý upload ảnh (cộng dồn hoặc thay thế? Tạm thời là cộng dồn hoặc nếu muốn
        // thay thế thì clear cũ đi)
        // Logic ở đây: Nếu có gửi ảnh mới lên, thì thêm vào danh sách hiện có.
        // (Nếu muốn xóa ảnh cũ, cần API riêng hoặc logic phức tạp hơn)
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

    @Override
    public void deleteRoom(Long id) {
        if (!roomRepository.findById(id).isPresent()) {
            throw new EntityNotFoundException("Không tìm thấy phòng: " + id);
        }
        // (Chúng ta nên check xem phòng có đang được đặt không trước khi xóa,
        // nhưng tạm thời cứ xóa)
        roomRepository.deleteById(id);
    }

    // BỔ SUNG: (US-26)
    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> findAvailableRooms(LocalDateTime startTime, LocalDateTime endTime, int capacity) {
        // 1. Gọi query phức tạp
        List<Room> availableRooms = roomRepository.findAvailableRooms(startTime, endTime, capacity);

        // 2. Map sang DTO
        return availableRooms.stream()
                .map(room -> modelMapper.map(room, RoomDTO.class))
                .collect(Collectors.toList());
    }
}