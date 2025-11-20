package com.cmc.meeting.domain.port.repository;

import com.cmc.meeting.domain.model.Room;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    List<Room> findAll();

    Optional<Room> findById(Long id);
    Room save(Room room);

    void deleteById(Long id);
    List<Room> findAvailableRooms(LocalDateTime startTime, LocalDateTime endTime, int capacity);

    Optional<Room> findByNameContainingIgnoreCase(String name);
}