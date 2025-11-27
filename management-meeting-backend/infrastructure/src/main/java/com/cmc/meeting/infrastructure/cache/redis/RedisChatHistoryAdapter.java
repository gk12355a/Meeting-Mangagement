package com.cmc.meeting.infrastructure.cache.redis;

import com.cmc.meeting.application.dto.chat.ChatMessage;
import com.cmc.meeting.application.port.cache.ChatHistoryPort;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RedisChatHistoryAdapter implements ChatHistoryPort {
private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper; // Inject ObjectMapper của Spring
    
    private static final String KEY_PREFIX = "chat_history:";
    private static final long TIMEOUT_MINUTES = 60;

    @Override
    public void addMessage(Long userId, String role, String content) {
        String key = KEY_PREFIX + userId;
        ChatMessage msg = new ChatMessage(role, content);
        
        // Push vào Redis
        redisTemplate.opsForList().rightPush(key, msg);
        redisTemplate.expire(key, TIMEOUT_MINUTES, TimeUnit.MINUTES);
        
        // Giới hạn 20 tin nhắn
        if (redisTemplate.opsForList().size(key) > 20) {
            redisTemplate.opsForList().leftPop(key);
        }
    }

    @Override
    public List<ChatMessage> getHistory(Long userId) {
        String key = KEY_PREFIX + userId;
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
        
        if (rawList == null || rawList.isEmpty()) {
            return new ArrayList<>();
        }

        // --- ĐOẠN FIX LỖI 500 Ở ĐÂY ---
        // Thay vì ép kiểu (ChatMessage) obj -> Sẽ lỗi LinkedHashMap cannot be cast...
        // Ta dùng objectMapper.convertValue để map dữ liệu sang Object chuẩn
        return rawList.stream()
                .map(obj -> objectMapper.convertValue(obj, ChatMessage.class)) 
                .collect(Collectors.toList());
    }

    @Override
    public void clearHistory(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}