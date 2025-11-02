package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataMeetingRepository extends JpaRepository<MeetingEntity, Long> {

    /**
     * Hiện thực logic kiểm tra trùng lịch (isRoomBusy)
     * Đây là query kiểm tra "khoảng thời gian chồng chéo" (Overlap)
     */
    @Query("SELECT COUNT(m) > 0 FROM MeetingEntity m " +
           "WHERE m.room.id = :roomId " +
           "AND m.status = 'CONFIRMED' " +
           "AND m.startTime < :endTime " + // Cuộc họp cũ kết thúc SAU khi cuộc họp mới bắt đầu
           "AND m.endTime > :startTime")   // Cuộc họp cũ bắt đầu TRƯỚC khi cuộc họp mới kết thúc
    boolean findRoomOverlap(@Param("roomId") Long roomId,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);
    @Query("SELECT DISTINCT m FROM MeetingEntity m " +
           "LEFT JOIN m.participants p " +
           "WHERE m.organizer.id = :userId OR p.userId = :userId") // <-- SỬA p.id THÀNH p.userId
    List<MeetingEntity> findAllByUserId(@Param("userId") Long userId);
    // BỔ SUNG: (US-22)
    // Tìm các cuộc họp đã 'CONFIRMED' nằm trong khoảng thời gian
    @Query("SELECT m FROM MeetingEntity m " +
           "WHERE m.status = 'CONFIRMED' " +
           "AND m.startTime >= :from AND m.endTime <= :to")
    List<MeetingEntity> findConfirmedMeetingsInDateRange(
            @Param("from") LocalDateTime from, 
            @Param("to") LocalDateTime to);
    @Query("SELECT m FROM MeetingEntity m JOIN m.participants p WHERE p.responseToken = :token")
    Optional<MeetingEntity> findMeetingByParticipantToken(@Param("token") String token);
}