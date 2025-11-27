package com.cmc.meeting.application.port.cache;

import com.cmc.meeting.application.dto.chat.ChatMessage;
import java.util.List;

public interface ChatHistoryPort {
    void addMessage(Long userId, String role, String content);
    List<ChatMessage> getHistory(Long userId);
    void clearHistory(Long userId);
}
