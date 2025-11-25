package com.cmc.meeting.domain.event;

/**
 * Sự kiện được kích hoạt khi một User mới được tạo (bởi Admin).
 * Chứa mật khẩu thô (rawPassword) để gửi email chào mừng.
 */
public class UserCreatedEvent {
    
    private final Long userId;
    private final String rawPassword;

    public UserCreatedEvent(Long userId, String rawPassword) {
        this.userId = userId;
        this.rawPassword = rawPassword;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRawPassword() {
        return rawPassword;
    }
}