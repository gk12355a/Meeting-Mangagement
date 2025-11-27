package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.chat.ChatResponse;
import com.cmc.meeting.application.dto.chat.StructuredIntent;
import com.cmc.meeting.application.dto.meeting.MeetingCancelRequest;
import com.cmc.meeting.application.dto.request.MeetingCreationRequest;
import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.application.port.llm.LanguageModelPort;
import com.cmc.meeting.application.port.service.ChatbotService;
import com.cmc.meeting.application.port.service.MeetingService;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.Room;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.domain.port.repository.RoomRepository;
import com.cmc.meeting.domain.port.repository.UserRepository; 
import com.cmc.meeting.application.port.cache.ChatHistoryPort;
import com.cmc.meeting.application.dto.chat.ChatMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.cmc.meeting.domain.model.RoomStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.List;

import org.springframework.data.domain.Page;


@Service
public class ChatbotServiceImpl implements ChatbotService {

    private final LanguageModelPort languageModelPort;
    private final MeetingService meetingService;
    private final RoomRepository roomRepository;
    private final MeetingRepository meetingRepository;
    private final ChatHistoryPort chatHistoryPort;
    private final UserRepository userRepository; 

    // Constructor Injection
    public ChatbotServiceImpl(LanguageModelPort languageModelPort,
                              MeetingService meetingService,
                              RoomRepository roomRepository,
                              MeetingRepository meetingRepository,
                              ChatHistoryPort chatHistoryPort,
                              UserRepository userRepository) { 
        this.languageModelPort = languageModelPort;
        this.meetingService = meetingService;
        this.roomRepository = roomRepository;
        this.meetingRepository = meetingRepository;
        this.chatHistoryPort = chatHistoryPort;
        this.userRepository = userRepository; 
    }

    @Override
    public ChatResponse processQuery(String query, Long userId) {
        List<ChatMessage> redisHistory = chatHistoryPort.getHistory(userId);
        User user = userRepository.findById(userId).orElseThrow();
        String userInfoContext = String.format("Tên: %s, ID: %d", user.getFullName(), user.getId());

        // Gọi AI
        StructuredIntent intent = languageModelPort.getStructuredIntent(query, redisHistory, userInfoContext);
        
        String replyMessage = intent.getReply(); // Mặc định lấy câu trả lời của AI
        String safeIntent = (intent.getIntent() != null) ? intent.getIntent().trim().toUpperCase() : "UNKNOWN";

        System.out.println("🔍 Intent: " + safeIntent);

        try {
            switch (safeIntent) {
                // TRƯỜNG HỢP 1: AI thấy thiếu thông tin -> AI tự hỏi lại (Logic nằm ở Prompt)
                case "GATHER_INFO":
                case "WAIT_CONFIRMATION": 
                case "UNKNOWN":
                    // Không làm gì cả, trả về câu reply của AI (Ví dụ: "Bạn muốn họp lúc mấy giờ?")
                    break;

                // TRƯỜNG HỢP 2: Đủ giờ/người -> Cần tìm phòng phù hợp
                case "FIND_ROOM":
                    replyMessage = handleFindAvailableRoom(intent);
                    break;

                // TRƯỜNG HỢP 3: Chốt đơn -> Đặt phòng
                case "EXECUTE_BOOKING":
                case "SCHEDULE_MEETING": // Hỗ trợ cả intent cũ
                    replyMessage = handleScheduleMeeting(intent, userId);
                    break;

                case "LIST_MEETINGS":
                    replyMessage = handleListMeetings(userId);
                    break;
                    
                case "CANCEL_MEETING":
                    replyMessage = handleCancelMeeting(intent, userId);
                    break;
                case "RESET":
                    // Xóa lịch sử trong Redis để Bot quên context cũ đi
                    chatHistoryPort.clearHistory(userId);
                    replyMessage = "✅ Đã hủy thao tác đặt phòng. Bạn cần giúp gì khác không?";
                    // Không lưu câu "Hủy" này vào history mới nữa để tránh nhiễu
                    return new ChatResponse(replyMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            replyMessage = "❌ Lỗi: " + e.getMessage();
        }
        
        // 4. Lưu lịch sử
        chatHistoryPort.addMessage(userId, "user", query);
        chatHistoryPort.addMessage(userId, "model", replyMessage);
        
        return new ChatResponse(replyMessage);
    }
    private String handleFindAvailableRoom(StructuredIntent intent) {
        // 1. Validate Thời gian (Giữ nguyên)
        if (intent.getStartTime() == null || intent.getEndTime() == null) {
            return "Tôi cần biết thời gian cụ thể (giờ bắt đầu - kết thúc) để kiểm tra phòng trống.";
        }

        // 2. SỬA ĐỔI: Validate Số người
        // Nếu AI không trích xuất được số người (null hoặc 0), HỎI LẠI NGAY thay vì đoán mò.
        if (intent.getParticipants() == null || intent.getParticipants() <= 0) {
            return "Cuộc họp này dự kiến có bao nhiêu người tham gia vậy bạn?";
        }
        
        int participants = intent.getParticipants();

        // 3. Logic tìm phòng
        List<Room> allRooms = roomRepository.findAll(); 
        
        List<Room> suitableRooms = allRooms.stream()
                // --- UPDATE 1: CHỈ LẤY PHÒNG ĐANG HOẠT ĐỘNG ---
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE) 
                // ----------------------------------------------
                
                // Check sức chứa
                .filter(r -> r.getCapacity() >= participants)
                
                // Check trùng lịch
                .filter(r -> isRoomAvailable(r.getId(), intent.getStartTime(), intent.getEndTime()))
                
                // Sắp xếp: Ưu tiên phòng nhỏ nhất vừa đủ (để tiết kiệm phòng lớn)
                .sorted(Comparator.comparingInt(Room::getCapacity)) 
                .toList();

        if (suitableRooms.isEmpty()) {
            return String.format("Rất tiếc, không có phòng nào TRỐNG hoặc ĐANG HOẠT ĐỘNG vào lúc %s phù hợp cho %d người.",
                    intent.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")), participants);
        }

        // Lấy phòng tốt nhất
        Room suggested = suitableRooms.get(0);
        
        // Lưu thông tin phòng vào context phản hồi để AI nắm được
        return String.format("✅ Tôi tìm thấy **%s** (Sức chứa: %d người) đang trống và phù hợp.\nBạn có muốn chốt đặt phòng này không?", 
                suggested.getName(), suggested.getCapacity());       
    }

    // ... Các hàm handleScheduleMeeting, handleListMeetings, handleCancelMeeting giữ nguyên như cũ ...
    private String handleScheduleMeeting(StructuredIntent intent, Long organizerId) {
        MeetingCreationRequest request = new MeetingCreationRequest();
        request.setTitle(intent.getTitle() != null ? intent.getTitle() : "Họp nhanh (từ Chatbot)");
        request.setDescription("Được tạo tự động bởi AI Chatbot");

        if (intent.getStartTime() == null || intent.getEndTime() == null) {
            throw new IllegalArgumentException("Thời gian không hợp lệ. Vui lòng cung cấp giờ bắt đầu và kết thúc.");
        }
        request.setStartTime(intent.getStartTime());
        request.setEndTime(intent.getEndTime());

        if (intent.getRoomName() != null && !intent.getRoomName().isEmpty()) {
            Optional<Room> room = roomRepository.findByNameContainingIgnoreCase(intent.getRoomName());
            if (room.isPresent()) {
                request.setRoomId(room.get().getId());
            } else {
                throw new IllegalArgumentException("Không tìm thấy phòng nào có tên là: " + intent.getRoomName());
            }
        } else {
             throw new IllegalArgumentException("Bạn muốn đặt phòng nào? Vui lòng nói tên phòng.");
        }

        Set<Long> participantIds = new HashSet<>();
        participantIds.add(organizerId);
        
        request.setParticipantIds(participantIds);
        request.setDeviceIds(new HashSet<>());
        request.setRecurrenceRule(null);

        MeetingDTO newMeeting = meetingService.createMeeting(request, organizerId);

        return String.format("✅ Đặt lịch thành công!\n📌 Phòng: %s\n⏰ Thời gian: %s - %s\n👤 Người tham dự: Bạn", 
                newMeeting.getRoom().getName(), 
                newMeeting.getStartTime(),
                newMeeting.getEndTime());
    }

    private String handleListMeetings(Long userId) {
        Page<MeetingDTO> meetings = meetingService.getMyMeetings(userId, PageRequest.of(0, 5));

        if (meetings.isEmpty()) {
            return "📅 Hôm nay bạn không có lịch họp nào sắp tới.";
        }

        StringBuilder sb = new StringBuilder("📅 **Lịch họp sắp tới của bạn:**\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");

        for (MeetingDTO m : meetings.getContent()) {
            sb.append(String.format("- **%s** lúc %s tại %s\n",
                    m.getTitle(),
                    m.getStartTime().format(fmt),
                    m.getRoom().getName()));
        }
        return sb.toString();
    }
    
    private String handleCancelMeeting(StructuredIntent intent, Long userId) {
        if (intent.getStartTime() == null) {
            return "⚠️ Tôi cần biết thời gian cuộc họp để hủy.";
        }
        List<Meeting> meetings = meetingRepository.findByOrganizerIdAndStartTime(userId, intent.getStartTime());
        if (meetings.isEmpty()) {
             DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");
             return String.format("⚠️ Không tìm thấy cuộc họp nào do bạn tổ chức bắt đầu lúc **%s**.", intent.getStartTime().format(fmt));
        }
        Meeting meeting = meetings.get(0);
        MeetingCancelRequest cancelRequest = new MeetingCancelRequest();
        cancelRequest.setReason(intent.getCancelReason() != null ? intent.getCancelReason() : "Hủy thông qua Chatbot AI");
        meetingService.cancelMeeting(meeting.getId(), cancelRequest, userId);
        return String.format("✅ Đã hủy thành công cuộc họp: **%s**", meeting.getTitle());
    }
    private boolean isRoomAvailable(Long roomId, LocalDateTime start, LocalDateTime end) {
        List<Meeting> conflicts = meetingRepository.findConflicts(roomId, start, end);
        return conflicts.isEmpty();
    }
}