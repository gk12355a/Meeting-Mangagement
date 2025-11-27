package com.cmc.meeting.application.port.llm;

import com.cmc.meeting.application.dto.chat.ChatMessage;
import com.cmc.meeting.application.dto.chat.StructuredIntent;
import java.util.List;

public interface LanguageModelPort {
    StructuredIntent getStructuredIntent(String query, List<ChatMessage> history, String userContextInfo);
}