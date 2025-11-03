package com.cmc.meeting.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PasswordResetToken {
    private Long id;
    private String token;
    private User user; // Token này thuộc về user nào
    private LocalDateTime expiryDate; // Thời gian hết hạn

    public PasswordResetToken(String token, User user, LocalDateTime expiryDate) {
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }
}