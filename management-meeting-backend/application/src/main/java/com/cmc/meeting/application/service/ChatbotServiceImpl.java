package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.chat.ChatResponse;
import com.cmc.meeting.application.dto.chat.StructuredIntent;
import com.cmc.meeting.application.dto.request.MeetingCreationRequest;
import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.application.port.llm.LanguageModelPort;
import com.cmc.meeting.application.port.service.ChatbotService;
import com.cmc.meeting.application.port.service.MeetingService;
import com.cmc.meeting.domain.model.Room;
import com.cmc.meeting.domain.port.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private final LanguageModelPort languageModelPort;
    private final MeetingService meetingService;
    private final RoomRepository roomRepository;

    // Constructor Injection
    public ChatbotServiceImpl(LanguageModelPort languageModelPort,
                              MeetingService meetingService,
                              RoomRepository roomRepository) {
        this.languageModelPort = languageModelPort;
        this.meetingService = meetingService;
        this.roomRepository = roomRepository;
    }

    @Override
    public ChatResponse processQuery(String query, Long userId) {
        // 1. Gọi LLM
        StructuredIntent intent = languageModelPort.getStructuredIntent(query);
        String replyMessage;

        try {
            // --- SỬA LỖI TẠI ĐÂY ---
            // Lấy intent, nếu null hoặc rỗng thì gán cứng là "UNKNOWN"
            String safeIntent = (intent.getIntent() != null) ? intent.getIntent().trim().toUpperCase() : "UNKNOWN";
            
            // Log ra console để bạn biết AI đang trả về cái gì (Quan trọng để debug)
            System.out.println("🔍 AI Raw Intent: " + intent.getIntent());
            System.out.println("✅ Safe Intent used: " + safeIntent);

            // 2. Điều phối
            switch (safeIntent) {
                case "SCHEDULE_MEETING":
                    replyMessage = handleScheduleMeeting(intent, userId);
                    break;
                    
                // Nếu AI trả về intent là "FIND_TIME" hoặc "BOOK_ROOM" (do nó tự sáng tác), 
                // ta có thể map nó về SCHEDULE_MEETING nếu muốn.
                case "BOOK_ROOM": 
                case "CREATE_MEETING":
                    replyMessage = handleScheduleMeeting(intent, userId);
                    break;

                case "UNKNOWN":
                default:
                    replyMessage = intent.getReply() != null 
                        ? intent.getReply() 
                        : "Xin lỗi, tôi không tìm thấy thông tin đặt phòng hợp lệ trong câu nói của bạn.";
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            replyMessage = "❌ Lỗi xử lý: " + e.getMessage();
        }

        return new ChatResponse(replyMessage);
    }

    private String handleScheduleMeeting(StructuredIntent intent, Long organizerId) {
        MeetingCreationRequest request = new MeetingCreationRequest();

        // --- 1. Map Tiêu đề & Thời gian ---
        request.setTitle(intent.getTitle() != null ? intent.getTitle() : "Họp nhanh (từ Chatbot)");
        request.setDescription("Được tạo tự động bởi AI Chatbot");
        
        if (intent.getStartTime() == null || intent.getEndTime() == null) {
            throw new IllegalArgumentException("Thời gian không hợp lệ. Vui lòng cung cấp giờ bắt đầu và kết thúc.");
        }
        request.setStartTime(intent.getStartTime());
        request.setEndTime(intent.getEndTime());

        // --- 2. Xử Lý Phòng Họp (Logic tìm ID từ Tên) ---
        // Nếu AI trích xuất được tên phòng (ví dụ: "Phòng Họp Lớn")
        if (intent.getRoomName() != null && !intent.getRoomName().isEmpty()) {
            // Tìm trong DB (không phân biệt hoa thường)
            Optional<Room> room = roomRepository.findByNameContainingIgnoreCase(intent.getRoomName());
            
            if (room.isPresent()) {
                request.setRoomId(room.get().getId()); // Set ID tìm được
            } else {
                throw new IllegalArgumentException("Không tìm thấy phòng nào có tên là: " + intent.getRoomName());
            }
        } else {
            // Nếu user không nói tên phòng
             throw new IllegalArgumentException("Bạn muốn đặt phòng nào? Vui lòng nói tên phòng.");
        }

        // --- 3. Xử Lý Người Tham Gia (Đơn giản hóa) ---
        Set<Long> participantIds = new HashSet<>();
        
        // Logic: "Tôi tạo thì tôi mời tôi" -> Chỉ thêm ID người tạo
        participantIds.add(organizerId);
        
        request.setParticipantIds(participantIds);
        request.setDeviceIds(new HashSet<>()); // Không yêu cầu thiết bị
        request.setRecurrenceRule(null); // Không lặp lại

        // --- 4. Gọi Service Tạo Cuộc Họp ---
        // Hàm này sẽ throw PolicyViolationException nếu phòng bận hoặc trùng lịch
        MeetingDTO newMeeting = meetingService.createMeeting(request, organizerId);

        return String.format("✅ Đặt lịch thành công!\n📌 Phòng: %s\n⏰ Thời gian: %s - %s\n👤 Người tham dự: Bạn", 
                newMeeting.getRoom().getName(), 
                newMeeting.getStartTime(),
                newMeeting.getEndTime());
    }
}