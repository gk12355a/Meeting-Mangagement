package com.cmc.meeting.application.dto.notification;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    private Long meetingId; // Chỉ trả về ID cuộc họp
}