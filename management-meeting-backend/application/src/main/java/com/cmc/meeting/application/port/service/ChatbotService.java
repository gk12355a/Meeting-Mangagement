package com.cmc.meeting.application.port.service;

import java.util.List;

import com.cmc.meeting.application.dto.chat.ChatResponse;

public interface ChatbotService {
    /**
     * Xử lý một câu lệnh chat từ người dùng đã được xác thực.
     *
     * @param query Câu lệnh của người dùng.
     * @param userId ID của người dùng đang thực hiện request.
     * @return Một đối tượng ChatResponse chứa câu trả lời.
     */
    ChatResponse processQuery(String query, Long userId);
}