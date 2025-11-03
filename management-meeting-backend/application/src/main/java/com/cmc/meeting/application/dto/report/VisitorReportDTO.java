package com.cmc.meeting.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorReportDTO {
    private Long meetingId;
    private String meetingTitle;
    private LocalDateTime startTime;
    private String roomName;
    private String organizerName; // Người tổ chức (để liên hệ)
    private Set<String> guestEmails; // Danh sách khách
}