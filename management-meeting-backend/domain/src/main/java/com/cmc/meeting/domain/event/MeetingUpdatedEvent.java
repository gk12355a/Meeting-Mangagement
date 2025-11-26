package com.cmc.meeting.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeetingUpdatedEvent {
    private Long meetingId;
    private Long userId;          // Người thực hiện sửa (Organizer)
    private String googleEventId; // ID sự kiện trên Google cần sửa
}