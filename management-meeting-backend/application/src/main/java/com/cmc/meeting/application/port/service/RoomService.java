package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.room.RoomRequest;
import com.cmc.meeting.application.dto.room.RoomDTO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomService {
    List<RoomDTO> getAllRooms();

    RoomDTO createRoom(RoomRequest request, List<MultipartFile> images);

    RoomDTO updateRoom(Long id, RoomRequest request, List<MultipartFile> images);

    void deleteRoom(Long id);

    List<RoomDTO> findAvailableRooms(LocalDateTime startTime, LocalDateTime endTime, int capacity);
}