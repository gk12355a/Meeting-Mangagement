package com.cmc.meeting.application.port.notification;
import com.cmc.meeting.domain.model.User;
public interface EmailNotificationPort {
    void sendHtmlEmail(String to, String subject, String htmlBody);
    void sendPasswordResetEmail(User user, String token);
    void sendWelcomeEmail(User user, String rawPassword);
}