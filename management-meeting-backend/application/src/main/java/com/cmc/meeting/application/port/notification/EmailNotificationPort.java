package com.cmc.meeting.application.port.notification;

public interface EmailNotificationPort {
    void sendHtmlEmail(String to, String subject, String htmlBody);
}