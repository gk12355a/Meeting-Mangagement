package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.MeetingEntity;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.RoomEntity;
import org.springframework.data.repository.query.Param; // <--- SỬA IMPORT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataRoomRepository extends JpaRepository<RoomEntity, Long> {

    // Method này dùng để check phòng trống (Logic của bạn rất tốt)
    @Query("SELECT r FROM RoomEntity r " +
           "WHERE r.capacity >= :capacity " +
           "AND r.id NOT IN (" +
           "  SELECT m.room.id FROM MeetingEntity m " +
           "  WHERE m.status = 'CONFIRMED' " +
           "  AND m.startTime < :endTime " +
           "  AND m.endTime > :startTime" +
           ")")
    List<RoomEntity> findAvailableRooms(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("capacity") int capacity);

    // BỔ SUNG: Để Chatbot tìm ID phòng từ tên (ví dụ: "Phòng A")
    Optional<RoomEntity> findByNameContainingIgnoreCase(String name);
}