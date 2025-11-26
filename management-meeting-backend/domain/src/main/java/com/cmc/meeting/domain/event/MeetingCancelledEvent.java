package com.cmc.meeting.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeetingCancelledEvent {
    private Long meetingId;
    private Long userId;          // Người thực hiện hủy
    private String googleEventId; // ID sự kiện trên Google cần xóa
}