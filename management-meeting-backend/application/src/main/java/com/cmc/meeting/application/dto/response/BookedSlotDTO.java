package com.cmc.meeting.application.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO đơn giản chỉ chứa thông tin cơ bản của một lịch họp
 * dùng để hiển thị trên lịch (calendar) của phòng.
 */
@Data
@NoArgsConstructor
public class BookedSlotDTO {
    
    private Long id; // ID của cuộc họp
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String organizerName; // Tên người tổ chức

    public BookedSlotDTO(Long id, String title, LocalDateTime startTime, LocalDateTime endTime, String organizerName) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.organizerName = organizerName;
    }
}