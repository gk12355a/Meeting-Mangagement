package com.cmc.meeting.application.port.llm;

import java.util.List;

import com.cmc.meeting.application.dto.chat.StructuredIntent;

public interface LanguageModelPort {
    /**
     * Phân tích một câu lệnh (query) của người dùng và trả về một
     * đối tượng StructuredIntent (ý định có cấu trúc).
     *
     * @param query Câu lệnh thô của người dùng.
     * @return Một đối tượng StructuredIntent.
     */
    StructuredIntent getStructuredIntent(String query, List<String> history);
}