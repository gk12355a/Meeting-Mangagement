package com.cmc.meeting.infrastructure.persistence.jpa.repository;

import com.cmc.meeting.infrastructure.persistence.jpa.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsRead(Long userId, boolean isRead);

    @Modifying // Báo cho Spring đây là query Cập nhật/Xóa
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);
}