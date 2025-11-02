package com.cmc.meeting.domain.event;

import java.time.LocalDateTime;

/**
 * Đây là một POJO Event thuần túy của Domain.
 * Nó KHÔNG extends ApplicationEvent hoặc bất cứ thứ gì của Spring.
 * Nó chỉ chứa dữ liệu.
 */
public class MeetingCreatedEvent {

    private final Long meetingId;
    private final LocalDateTime timestamp;

    // Bỏ constructor cũ, dùng constructor này
    public MeetingCreatedEvent(Long meetingId) {
        this.meetingId = meetingId;
        this.timestamp = LocalDateTime.now(); // Ghi lại thời điểm sự kiện xảy ra
    }

    // Getters
    public Long getMeetingId() {
        return meetingId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}