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

    public RoomServiceImpl(RoomRepository roomRepository, ModelMapper modelMapper) {
        this.roomRepository = roomRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(room -> modelMapper.map(room, RoomDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomDTO createRoom(RoomRequest request) {
        Room newRoom = modelMapper.map(request, Room.class);
        // newRoom.setRequiredRoles(request.getRequiredRoles()); // ModelMapper tự làm
        Room savedRoom = roomRepository.save(newRoom);
        return modelMapper.map(savedRoom, RoomDTO.class);
    }

    @Override
    public RoomDTO updateRoom(Long id, RoomRequest request) {
        Room existingRoom = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng: " + id));

        // Cập nhật các trường
        existingRoom.setName(request.getName());
        existingRoom.setCapacity(request.getCapacity());
        existingRoom.setLocation(request.getLocation());
        existingRoom.setFixedDevices(request.getFixedDevices());
        existingRoom.setRequiredRoles(request.getRequiredRoles());
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