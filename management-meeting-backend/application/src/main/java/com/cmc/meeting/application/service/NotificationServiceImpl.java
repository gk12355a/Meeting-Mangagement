package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.notification.NotificationDTO;
import com.cmc.meeting.application.port.service.NotificationService;
import com.cmc.meeting.domain.exception.PolicyViolationException;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.Notification;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository, ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.modelMapper = modelMapper;
    }

    // Tạo thông báo (cho Service khác gọi)
    @Override
    public void createNotification(User user, String message, Meeting meeting) {
        Notification notification = new Notification(message, user, meeting);
        notificationRepository.save(notification);
    }

    // Lấy danh sách (có phân trang)
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getMyNotifications(Long currentUserId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable);
        return page.map(notification -> modelMapper.map(notification, NotificationDTO.class));
    }

    // Đếm số lượng chưa đọc
    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getUnreadCount(Long currentUserId) {
        long count = notificationRepository.countByUserIdAndIsRead(currentUserId, false);
        return Map.of("unreadCount", count);
    }

    // Đánh dấu 1 cái đã đọc
    @Override
    public NotificationDTO markAsRead(Long notificationId, Long currentUserId) {
        Notification notification = findNotification(notificationId, currentUserId);
        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return modelMapper.map(saved, NotificationDTO.class);
    }

    // Đánh dấu tất cả đã đọc
    @Override
    public void markAllAsRead(Long currentUserId) {
        notificationRepository.markAllAsReadByUserId(currentUserId);
    }

    // Helper kiểm tra quyền sở hữu
    private Notification findNotification(Long notificationId, Long currentUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông báo"));

        if (!notification.getUser().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Bạn không có quyền xem thông báo này.");
        }
        return notification;
    }
}