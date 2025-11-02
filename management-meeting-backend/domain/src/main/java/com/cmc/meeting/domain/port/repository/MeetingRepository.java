package com.cmc.meeting.domain.port.repository;

import com.cmc.meeting.domain.model.Meeting;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Đây là "Hợp đồng" (Contract)
// Nó không biết gì về JPA hay MyBatiS
public interface MeetingRepository {

    Meeting save(Meeting meeting);

    Optional<Meeting> findById(Long id);

    // Sẽ cần thêm các method phức tạp sau
    // ví dụ: boolean isRoomBusy(Long roomId, LocalDateTime start, LocalDateTime end);
    boolean isRoomBusy(Long roomId, LocalDateTime startTime, LocalDateTime endTime);
    List<Meeting> findAllByUserId(Long userId);
}