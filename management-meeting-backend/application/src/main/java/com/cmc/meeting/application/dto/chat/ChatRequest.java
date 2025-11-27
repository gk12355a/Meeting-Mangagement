package com.cmc.meeting.application.dto.chat;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatRequest {
    private String query;
    private List<String> history;
}