package com.cmc.meeting.application.dto.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
// Bỏ qua các trường lạ mà LLM có thể trả về
@JsonIgnoreProperties(ignoreUnknown = true) 
public class StructuredIntent {
    private String intent; 
    private Integer participants;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String title;
    private String roomName;
    private String reply; 
    private String cancelReason;
    private String filterType;
    private List<String> devices;
}