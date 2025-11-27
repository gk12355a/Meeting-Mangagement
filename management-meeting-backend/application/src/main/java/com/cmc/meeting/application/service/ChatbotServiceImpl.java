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
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.domain.port.repository.RoomRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
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

    // Constructor Injection
    public ChatbotServiceImpl(LanguageModelPort languageModelPort,
                              MeetingService meetingService,
                              RoomRepository roomRepository,
                              MeetingRepository meetingRepository) {
        this.languageModelPort = languageModelPort;
        this.meetingService = meetingService;
        this.roomRepository = roomRepository;
        this.meetingRepository = meetingRepository;
    }

   @Override
    public ChatResponse processQuery(String query, List<String> history, Long userId) {
    // 1. Truyền history vào Adapter
    StructuredIntent intent = languageModelPort.getStructuredIntent(query, history);
        String replyMessage;
        try {
            String safeIntent = (intent.getIntent() != null) ? intent.getIntent().trim().toUpperCase() : "UNKNOWN";
            
            // Log để kiểm tra xem AI trả về gì
            System.out.println("🔍 Intent AI: " + safeIntent);

            switch (safeIntent) {
                case "SCHEDULE_MEETING":
                case "BOOK_ROOM":
                    replyMessage = handleScheduleMeeting(intent, userId);
                    break;

                // --- THÊM ĐOẠN NÀY ---
                case "LIST_MEETINGS":
                    replyMessage = handleListMeetings(userId);
                    break;
                // ---------------------

                case "UNKNOWN":
                default:
                    replyMessage = intent.getReply() != null 
                        ? intent.getReply() 
                        : "Xin lỗi, tôi chưa hiểu. Bạn muốn 'Đặt lịch' hay 'Xem lịch'?";
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            replyMessage = "❌ Lỗi: " + e.getMessage();
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
    /**
     * CHỨC NĂNG 1: Xem danh sách cuộc họp
     */
   private String handleListMeetings(Long userId) {
        // Gọi Service lấy danh sách (Page 0, lấy 5 cái mới nhất)
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

    /**
     * CHỨC NĂNG 2: Hủy cuộc họp
     */
    private String handleCancelMeeting(StructuredIntent intent, Long userId) {
        // 1. Kiểm tra đầu vào từ AI
        if (intent.getStartTime() == null) {
            return "⚠️ Tôi cần biết thời gian cuộc họp để hủy. Ví dụ: 'Hủy cuộc họp lúc 2 giờ chiều nay'.";
        }

        // 2. Tìm cuộc họp trong DB
        // Logic: Tìm cuộc họp do User tổ chức, bắt đầu đúng vào giờ AI trích xuất
        List<Meeting> meetings = meetingRepository.findByOrganizerIdAndStartTime(userId, intent.getStartTime());

        if (meetings.isEmpty()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");
            return String.format("⚠️ Không tìm thấy cuộc họp nào do bạn tổ chức bắt đầu lúc **%s**.", 
                    intent.getStartTime().format(fmt));
        }

        // 3. Thực hiện hủy (Lấy cuộc họp đầu tiên tìm thấy)
        Meeting meeting = meetings.get(0);
        
        MeetingCancelRequest cancelRequest = new MeetingCancelRequest();
        cancelRequest.setReason(intent.getCancelReason() != null 
                ? intent.getCancelReason() 
                : "Hủy thông qua Chatbot AI");

        // Gọi MeetingService để chạy logic nghiệp vụ (gửi mail, check quyền...)
        meetingService.cancelMeeting(meeting.getId(), cancelRequest, userId);

        return String.format("✅ Đã hủy thành công cuộc họp: **%s**", meeting.getTitle());
    }
}
