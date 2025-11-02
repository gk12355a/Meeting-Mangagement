package com.cmc.meeting.infrastructure.persistence.jpa.repository;
import com.cmc.meeting.infrastructure.persistence.jpa.entity.RoomEntity;

import io.lettuce.core.dynamic.annotation.Param;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List; // Đảm bảo đã import
public interface SpringDataRoomRepository extends JpaRepository<RoomEntity, Long> {
    @Query("SELECT r FROM RoomEntity r " +
           "WHERE r.capacity >= :capacity " +
           "AND r.id NOT IN (" +
           "  SELECT m.room.id FROM MeetingEntity m " + // Chọn các phòng đã bận
           "  WHERE m.status = 'CONFIRMED' " +
           "  AND m.startTime < :endTime " + // (Logic kiểm tra trùng lịch)
           "  AND m.endTime > :startTime" +
           ")")
    List<RoomEntity> findAvailableRooms(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("capacity") int capacity);
}