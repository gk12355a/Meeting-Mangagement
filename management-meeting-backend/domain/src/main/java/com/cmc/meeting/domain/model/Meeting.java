package com.cmc.meeting.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class Meeting {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Room room; // Phòng họp được đặt
    private User organizer; // Người tổ chức

    private Set<User> participants = new HashSet<>(); // Người tham dự
    private BookingStatus status;

    // Constructor cho nghiệp vụ tạo mới
    public Meeting(String title, LocalDateTime startTime, LocalDateTime endTime, Room room, User organizer, Set<User> participants) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.organizer = organizer;
        this.participants = participants;
        this.status = BookingStatus.CONFIRMED; // Mặc định là đã xác nhận
    }

    // --- QUY TẮC NGHIỆP VỤ (Business Rules) ---

    /**
     * Nghiệp vụ Hủy cuộc họp (US-2)
     */
    public void cancelMeeting() {
        if (this.status == BookingStatus.CANCELLED) {
            // Không thể hủy một cuộc họp đã bị hủy
            throw new IllegalStateException("Meeting is already cancelled.");
        }
        if (this.startTime.isBefore(LocalDateTime.now())) {
            // Không thể hủy một cuộc họp đã diễn ra
            throw new IllegalStateException("Cannot cancel a meeting that has already passed.");
        }
        this.status = BookingStatus.CANCELLED;
    }

    /**
     * Nghiệp vụ Thêm người tham dự (US-4)
     */
    public void addParticipant(User user) {
        if (this.status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot add participants to a non-confirmed meeting.");
        }
        this.participants.add(user);
    }
}