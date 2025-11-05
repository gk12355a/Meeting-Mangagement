package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.notification.NotificationDTO;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;

public interface NotificationService {
    // API cho Frontend
    Page<NotificationDTO> getMyNotifications(Long currentUserId, Pageable pageable);
    Map<String, Long> getUnreadCount(Long currentUserId);
    NotificationDTO markAsRead(Long notificationId, Long currentUserId);
    void markAllAsRead(Long currentUserId);

    // API cho Backend (Service nội bộ)
    void createNotification(User user, String message, Meeting meeting);
}