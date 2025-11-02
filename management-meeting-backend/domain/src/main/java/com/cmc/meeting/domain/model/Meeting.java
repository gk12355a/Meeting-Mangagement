package com.cmc.meeting.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.cmc.meeting.domain.exception.PolicyViolationException;
// import com.cmc.meeting.domain.model.MeetingParticipant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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

    private Set<MeetingParticipant> participants = new HashSet<>(); // Người tham dự
    private BookingStatus status;
    private boolean isCheckedIn = false;
    private String cancelReason;
    private LocalDateTime cancelledAt;

    // Constructor cho nghiệp vụ tạo mới
    public Meeting(String title, LocalDateTime startTime, LocalDateTime endTime,
            Room room, User organizer, Set<User> participantUsers) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.organizer = organizer;
        this.status = BookingStatus.CONFIRMED;
        this.isCheckedIn = false;

        // Tự động thêm Người tổ chức (Organizer) là ACCEPTED
        this.participants.add(
            new MeetingParticipant(organizer, ParticipantStatus.ACCEPTED, null) // Token là null
        );
        
        participantUsers.forEach(user -> {
            if (!user.getId().equals(organizer.getId())) {
                // Gán token ngẫu nhiên cho người PENDING
                this.participants.add(
                    new MeetingParticipant(user, ParticipantStatus.PENDING, UUID.randomUUID().toString())
                );
            }
        });
    }

    // --- QUY TẮC NGHIỆP VỤ (Business Rules) ---
    public void checkIn() {
        if (this.status != BookingStatus.CONFIRMED) {
            throw new PolicyViolationException("Cuộc họp đã bị hủy, không thể check-in.");
        }
        if (this.isCheckedIn) {
            throw new PolicyViolationException("Cuộc họp đã được check-in rồi.");
        }
        this.isCheckedIn = true;
    }

    /**
     * Nghiệp vụ Hủy cuộc họp (US-2)
     */
    public void cancelMeeting(String reason) { // <-- Thêm 'reason'
        if (this.status == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cuộc họp đã bị hủy.");
        }
        if (this.startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Không thể hủy cuộc họp đã diễn ra.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do hủy là bắt buộc.");
        }

        this.status = BookingStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * Nghiệp vụ Thêm người tham dự (US-4)
     */

    public void respondToInvitation(User user, ParticipantStatus newStatus) {
        MeetingParticipant participant = this.participants.stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(
                        () -> new PolicyViolationException("Bạn không có trong danh sách khách mời của cuộc họp này."));

        if (newStatus == ParticipantStatus.PENDING) {
            throw new IllegalArgumentException("Không thể đổi trạng thái về PENDING.");
        }

        participant.setStatus(newStatus);
    }
}