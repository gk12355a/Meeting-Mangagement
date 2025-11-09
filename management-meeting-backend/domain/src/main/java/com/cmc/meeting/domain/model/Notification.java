package com.cmc.meeting.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Notification {
    private Long id;
    private String message;     // Nội dung (vd: "User A đã chấp nhận...")
    
    // Đặt giá trị mặc định
    private boolean isRead = false;
    
    private LocalDateTime createdAt;
    private User user;          // Thông báo này của ai
    private Meeting meeting;    // (Tùy chọn) Link tới cuộc họp liên quan

    /**
     * Constructor (Hàm khởi tạo)
     * Đây là nơi sửa lỗi
     */
    public Notification(String message, User user, Meeting meeting) {
        this.message = message;
        this.user = user;
        this.meeting = meeting;
        this.createdAt = LocalDateTime.now();
        
        // SỬA LỖI: Luôn đặt trạng thái "chưa đọc" là false khi tạo mới
        this.isRead = false; 
    }
}