package com.cmc.meeting.infrastructure.event;

import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import com.cmc.meeting.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;

@Component
public class UserSyncListener {

    private static final Logger log = LoggerFactory.getLogger(UserSyncListener.class);
    private final UserRepository userRepository;

    public UserSyncListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveUserSync(Map<String, Object> userData) {
        try {
            String username = (String) userData.get("username");
            String fullName = (String) userData.get("fullName");

            log.info("🐰 RabbitMQ: Nhận yêu cầu đồng bộ user: {}", username);

            if (userRepository.findByUsername(username).isEmpty()) {
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setFullName(fullName);
                newUser.setPassword("DUMMY_PASS_SYNCED"); // Pass giả
                newUser.setActive(true);
                newUser.setRoles(new HashSet<>()); // Mặc định role rỗng hoặc ROLE_USER

                userRepository.save(newUser);
                log.info("✅ Đã đồng bộ User '{}' vào Meeting DB.", username);
            } else {
                log.info("User '{}' đã tồn tại, bỏ qua.", username);
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi đồng bộ user từ RabbitMQ", e);
        }
    }
}