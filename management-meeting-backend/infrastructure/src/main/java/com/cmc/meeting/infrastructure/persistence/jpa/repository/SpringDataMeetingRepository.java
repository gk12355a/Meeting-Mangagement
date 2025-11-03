package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SpringDataMeetingRepository extends JpaRepository<MeetingEntity, Long> {

       /**
        * Hiện thực logic kiểm tra trùng lịch (isRoomBusy)
        * Đây là query kiểm tra "khoảng thời gian chồng chéo" (Overlap)
        */
       @Query("SELECT COUNT(m) > 0 FROM MeetingEntity m " +
                     "WHERE m.room.id = :roomId " +
                     "AND m.status = 'CONFIRMED' " +
                     "AND m.startTime < :endTime " + // Cuộc họp cũ kết thúc SAU khi cuộc họp mới bắt đầu
                     "AND m.endTime > :startTime") // Cuộc họp cũ bắt đầu TRƯỚC khi cuộc họp mới kết thúc
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

       @Query("SELECT m FROM MeetingEntity m " +
                     "WHERE m.organizer.id = :organizerId " +
                     "AND m.room.id = :roomId " +
                     "AND m.status = 'CONFIRMED' " +
                     "AND m.isCheckedIn = false " +
                     // Điều kiện thời gian:
                     // (Từ 15 phút trước giờ họp ĐẾN 30 phút sau giờ họp)
                     "AND m.startTime BETWEEN :timeStartWindow AND :timeEndWindow")
       Optional<MeetingEntity> findCheckInEligibleMeeting(
                     @Param("organizerId") Long organizerId,
                     @Param("roomId") Long roomId,
                     @Param("timeStartWindow") LocalDateTime timeStartWindow, // vd: now - 15 phút
                     @Param("timeEndWindow") LocalDateTime timeEndWindow); // vd: now + 30 phút

       @Query("SELECT m FROM MeetingEntity m " +
                     "WHERE m.status = 'CONFIRMED' " +
                     "AND m.isCheckedIn = false " +
                     // startTime đã qua thời gian cutoff (vd: 15 phút)
                     "AND m.startTime < :cutoffTime")
       List<MeetingEntity> findUncheckedInMeetings(@Param("cutoffTime") LocalDateTime cutoffTime);

       // BỔ SUNG: (US-5)
       @Query("SELECT m FROM MeetingEntity m JOIN m.participants p " +
                     "WHERE m.status = 'CONFIRMED' " +
                     // Thời gian họp (m) chồng chéo với khoảng thời gian tìm kiếm (from, to)
                     "AND m.startTime < :to AND m.endTime > :from " +
                     // VÀ (p) là một trong các user trong danh sách
                     "AND p.userId IN :userIds")
       List<MeetingEntity> findMeetingsForUsersInDateRange(
                     @Param("userIds") Set<Long> userIds,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);

       // BỔ SUNG: (US-23)
       // Tìm các cuộc họp đã 'CANCELLED' trong khoảng thời gian
       @Query("SELECT m FROM MeetingEntity m " +
                     "WHERE m.status = 'CANCELLED' " +
                     "AND m.cancelledAt >= :from AND m.cancelledAt <= :to")
       List<MeetingEntity> findCanceledMeetingsInDateRange(
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);

       List<MeetingEntity> findAllBySeriesId(String seriesId);

       // BỔ SUNG: (BS-31)
       @Query("SELECT m FROM MeetingEntity m " +
                     "WHERE m.status = 'CONFIRMED' " +
                     "AND m.startTime >= :from AND m.startTime <= :to " +
                     "AND m.guestEmails IS NOT EMPTY") // Chỉ lấy cuộc họp có khách
       List<MeetingEntity> findMeetingsWithGuestsInDateRange(
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);
}