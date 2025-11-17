package com.cmc.meeting.application.service;

import com.cmc.meeting.application.port.llm.LanguageModelPort;
import com.cmc.meeting.application.port.service.ChatbotService;
import com.cmc.meeting.application.port.service.MeetingService;
import com.cmc.meeting.application.port.service.TimeSuggestionService;
import com.cmc.meeting.application.dto.chat.ChatResponse;
import com.cmc.meeting.application.dto.chat.StructuredIntent;
import com.cmc.meeting.application.dto.request.MeetingCreationRequest; // DTO đã có
import com.cmc.meeting.application.dto.timeslot.TimeSuggestionRequest; // DTO đã có
import com.cmc.meeting.application.dto.timeslot.TimeSlotDTO; // DTO đã có
import com.cmc.meeting.application.dto.response.MeetingDTO; // DTO đã có

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private final LanguageModelPort languageModelPort;
    private final MeetingService meetingService;
    private final TimeSuggestionService timeSuggestionService;

    public ChatbotServiceImpl(LanguageModelPort languageModelPort,
                              MeetingService meetingService,
                              TimeSuggestionService timeSuggestionService) {
        this.languageModelPort = languageModelPort;
        this.meetingService = meetingService;
        this.timeSuggestionService = timeSuggestionService;
    }

    @Override
    public ChatResponse processQuery(String query, Long userId) {
        // 1. Gọi LLM để phân tích ý định
        StructuredIntent intent = languageModelPort.getStructuredIntent(query);

        String replyMessage;

        // 2. Điều phối dựa trên ý định
        try {
            switch (intent.getIntent()) {
                case "SCHEDULE_MEETING":
                    replyMessage = handleScheduleMeeting(intent, userId);
                    break;
                case "UNKNOWN":
                default:
                    replyMessage = intent.getReply() != null ? intent.getReply() : "Xin lỗi, tôi không hiểu yêu cầu của bạn.";
                    break;
            }
        } catch (Exception e) {
            // Bắt các lỗi nghiệp vụ (ví dụ: PolicyViolationException)
            replyMessage = "Đã xảy ra lỗi khi xử lý yêu cầu: " + e.getMessage();
        }

        return new ChatResponse(replyMessage);
    }

    private String handleScheduleMeeting(StructuredIntent intent, Long organizerId) {
        // TODO: Chuyển đổi 'intent' thành 'MeetingCreationRequest'
        // Có thể bạn cần gọi 'handleSuggestRoom' trước để tìm 1 phòng
        
        MeetingCreationRequest request = new MeetingCreationRequest();
        request.setTitle(intent.getTitle() != null ? intent.getTitle() : "Cuộc họp (từ Chatbot)");
        request.setStartTime(intent.getStartTime());
        request.setEndTime(intent.getEndTime());
        
        // TODO: Bạn cần một logic để chọn phòng (roomId)
        // request.setRoomId(1L); // Ví dụ
        
        // TODO: Bạn cần một logic để thêm người tham dự
        // request.setParticipantIds(List.of(organizerId, ...));

        MeetingDTO newMeeting = meetingService.createMeeting(request, organizerId);
        
        return "Đã đặt lịch họp thành công tại phòng " + newMeeting.getRoom().getName() +
               " vào lúc " + newMeeting.getStartTime();
    }
}