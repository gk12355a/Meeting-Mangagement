package com.cmc.meeting.infrastructure.event;

import com.cmc.meeting.application.port.notification.EmailNotificationPort;
import com.cmc.meeting.domain.event.UserCreatedEvent;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    @Autowired
    private EmailNotificationPort emailSender;

    @Autowired
    private UserRepository userRepository;

    /**
     * Lắng nghe sự kiện tạo User (Sau khi CSDL đã commit)
     * và gọi Port để gửi email chào mừng.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreation(UserCreatedEvent event) {
        log.info("EVENT RECEIVED [Async, After Commit]: Xử lý sự kiện tạo user (ID: {}).", event.getUserId());
        
        try {
            User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user cho event"));
            
            // Gọi Port
            emailSender.sendWelcomeEmail(user, event.getRawPassword());
            
            log.info("-> Đã gửi email chào mừng cho: {}", user.getUsername());

        } catch (Exception e) {
            log.error("Lỗi khi xử lý gửi email chào mừng cho User ID {}: {}", event.getUserId(), e.getMessage());
        }
    }
}