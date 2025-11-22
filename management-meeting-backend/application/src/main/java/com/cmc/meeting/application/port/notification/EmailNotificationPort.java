package com.cmc.meeting.application.port.notification;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.User;
public interface EmailNotificationPort {
    void sendHtmlEmail(String to, String subject, String htmlBody);
    void sendPasswordResetEmail(User user, String token);
    void sendWelcomeEmail(User user, String rawPassword);
    // Hàm gửi nhắc nhở cuộc họp (Dùng chung cho cả User nội bộ và Guest)
    void sendMeetingReminder(String toEmail, Meeting meeting, String timeLabel);
}