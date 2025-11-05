package com.cmc.meeting.domain.port.repository;

import com.cmc.meeting.domain.model.Notification;
import org.springframework.data.domain.Page; // Import
import org.springframework.data.domain.Pageable; // Import

import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);

    // (API: GET /notifications)
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // (API: GET /unread-count)
    long countByUserIdAndIsRead(Long userId, boolean isRead);

    // (API: POST /read-all)
    void markAllAsReadByUserId(Long userId);
}