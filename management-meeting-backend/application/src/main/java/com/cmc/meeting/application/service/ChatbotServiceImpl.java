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
import com.cmc.meeting.domain.model.BookingStatus; // <--- Import mới
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
import java.util.ArrayList;
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

        StructuredIntent intent = languageModelPort.getStructuredIntent(query, redisHistory, userInfoContext);
        
        String replyMessage = intent.getReply();
        String safeIntent = (intent.getIntent() != null) ? intent.getIntent().trim().toUpperCase() : "UNKNOWN";

        System.out.println("🔍 Intent: " + safeIntent);

        try {
            switch (safeIntent) {
                case "GATHER_INFO":
                case "WAIT_CONFIRMATION": 
                case "UNKNOWN":
                    break;

                case "FIND_ROOM":
                    replyMessage = handleFindAvailableRoom(intent);
                    break;

                case "EXECUTE_BOOKING":
                case "SCHEDULE_MEETING":
                    replyMessage = handleScheduleMeeting(intent, userId);
                    break;

                case "LIST_MEETINGS":
                    // SỬA: Truyền cả intent vào để lấy filterType và time
                    replyMessage = handleListMeetings(userId, intent);
                    break;
                    
                case "CANCEL_MEETING":
                    replyMessage = handleCancelMeeting(intent, userId);
                    break;

                case "RESET":
                    chatHistoryPort.clearHistory(userId);
                    replyMessage = "✅ Đã hủy thao tác. Bạn cần giúp gì khác không?";
                    return new ChatResponse(replyMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            replyMessage = "❌ Lỗi: " + e.getMessage();
        }
        
        chatHistoryPort.addMessage(userId, "user", query);
        chatHistoryPort.addMessage(userId, "model", replyMessage);
        
        return new ChatResponse(replyMessage);
    }

    // --- HÀM XỬ LÝ LIST MEETINGS MỚI (LINH HOẠT HƠN) ---
    private String handleListMeetings(Long userId, StructuredIntent intent) {
        String filterType = intent.getFilterType() != null ? intent.getFilterType() : "UPCOMING";
        LocalDateTime from = intent.getStartTime();
        LocalDateTime to = intent.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        
        List<Meeting> meetings;
        String titleHeader;

        // Xử lý mặc định thời gian nếu AI chưa tính toán
        if (from == null) from = now;
        if (to == null) to = now.plusYears(1); // Mặc định lấy xa xa

        switch (filterType) {
            case "CANCELLED":
                // Lấy các cuộc họp đã hủy (trong 30 ngày gần đây nếu không chỉ định ngày)
                LocalDateTime cancelFrom = (intent.getStartTime() != null) ? intent.getStartTime() : now.minusDays(30);
                meetings = meetingRepository.findMeetingsByFilter(userId, cancelFrom, to, true);
                titleHeader = "🗑️ **Các cuộc họp ĐÃ HỦY:**";
                break;

            case "PAST":
                // Lịch sử: Lấy từ 30 ngày trước đến hiện tại
                LocalDateTime pastFrom = (intent.getStartTime() != null) ? intent.getStartTime() : now.minusDays(30);
                meetings = meetingRepository.findMeetingsByFilter(userId, pastFrom, now, false);
                titleHeader = "clock_history **Lịch sử cuộc họp (Đã qua):**";
                break;

            case "SPECIFIC_RANGE":
                // Xem theo ngày/tháng cụ thể user yêu cầu
                if (intent.getStartTime() == null || intent.getEndTime() == null) {
                    return "⚠️ Tôi cần biết khoảng thời gian cụ thể (VD: hôm nay, tuần này).";
                }
                meetings = meetingRepository.findMeetingsByFilter(userId, intent.getStartTime(), intent.getEndTime(), false);
                
                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM");
                titleHeader = String.format("📅 **Lịch họp từ %s đến %s:**", 
                        intent.getStartTime().format(dateFmt), intent.getEndTime().format(dateFmt));
                break;

            case "UPCOMING":
            default:
                // Mặc định: Lấy tương lai (Start > Now)
                meetings = meetingRepository.findMeetingsByFilter(userId, now, to, false);
                // Giới hạn 5 cuộc họp sắp tới để không bị spam
                if (meetings.size() > 5) {
                    meetings = meetings.subList(0, 5);
                }
                titleHeader = "📅 **Lịch họp sắp tới của bạn:**";
                break;
        }

        if (meetings.isEmpty()) {
            return titleHeader + "\n_(Không tìm thấy cuộc họp nào)_";
        }

        StringBuilder sb = new StringBuilder(titleHeader + "\n");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFmtFull = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Meeting m : meetings) {
            String statusIcon = (m.getStatus() == BookingStatus.CANCELLED) ? "❌" : "✅";
            sb.append(String.format("%s **%s**\n   🕒 %s - %s\n   📍 %s\n",
                    statusIcon,
                    m.getTitle(),
                    m.getStartTime().format(timeFmt),
                    m.getStartTime().format(dateFmtFull),
                    m.getRoom().getName()));
        }
        return sb.toString();
    }
    private String handleScheduleMeeting(StructuredIntent intent, Long organizerId) {
        MeetingCreationRequest request = new MeetingCreationRequest();
        request.setTitle(intent.getTitle() != null ? intent.getTitle() : "Họp nhanh (từ Chatbot)");
        
        // Cải thiện description
        String desc = "Được tạo tự động bởi AI Chatbot.";
        if (intent.getParticipants() != null && intent.getParticipants() > 1) {
            desc += String.format(" Dự kiến số lượng: %d người.", intent.getParticipants());
        }
        request.setDescription(desc);

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
    private String handleFindAvailableRoom(StructuredIntent intent) {
        // 1. Validate Thời gian & Số người (Giữ nguyên code cũ)
        if (intent.getStartTime() == null || intent.getEndTime() == null) {
            return "Tôi cần biết thời gian cụ thể (giờ bắt đầu - kết thúc) để kiểm tra phòng trống.";
        }
        
        // Mặc định là 0 để logic lọc bên dưới xử lý (hoặc giữ nguyên logic check null của bạn)
        int participants = (intent.getParticipants() != null) ? intent.getParticipants() : 0; 
        // Cập nhật số lượng người thực tế (Người tạo + Số người tìm thấy)
        // Hoặc lấy max giữa con số user nói và số lượng tìm thấy
        List<String> requiredDevices = intent.getDevices();

        // 3. Logic tìm phòng nâng cao
        List<Room> allRooms = roomRepository.findAll(); 
        
        List<Room> suitableRooms = allRooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                // Filter theo số người (nếu có yêu cầu)
                .filter(r -> participants == 0 || r.getCapacity() >= participants)
                // Check trùng lịch
                .filter(r -> isRoomAvailable(r.getId(), intent.getStartTime(), intent.getEndTime()))
                
                // --- BỘ LỌC THIẾT BỊ (MỚI) ---
                .filter(r -> hasAllDevices(r, requiredDevices))
                // -----------------------------
                
                .sorted(Comparator.comparingInt(Room::getCapacity)) 
                .toList();

        // Xử lý thông báo kết quả (Logic thông minh hơn)
        if (suitableRooms.isEmpty()) {
            String deviceMsg = (requiredDevices != null && !requiredDevices.isEmpty()) 
                    ? " có đủ các thiết bị: " + String.join(", ", requiredDevices) 
                    : "";
            
            return String.format("Rất tiếc, không có phòng nào trống vào lúc %s phù hợp cho %d người%s.",
                    intent.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")), 
                    participants, 
                    deviceMsg);
        }

        Room suggested = suitableRooms.get(0);
        
        // Tạo thông báo gợi ý có nhắc đến thiết bị
        String deviceNote = "";
        if (requiredDevices != null && !requiredDevices.isEmpty()) {
            deviceNote = "\n(Đã bao gồm: " + String.join(", ", requiredDevices) + ")";
        }

        return String.format("✅ Tôi tìm thấy **%s** (Sức chứa: %d người)%s đang trống.\nBạn có muốn chốt đặt phòng này không?", 
                suggested.getName(), suggested.getCapacity(), deviceNote);       
    }
    private boolean hasAllDevices(Room room, List<String> requiredDevices) {
        // Nếu user không yêu cầu thiết bị gì -> Phòng nào cũng thỏa mãn -> Return true
        if (requiredDevices == null || requiredDevices.isEmpty()) {
            return true;
        }

        // Nếu phòng không có thiết bị nào -> Return false
        if (room.getFixedDevices() == null || room.getFixedDevices().isEmpty()) {
            return false;
        }

        // Kiểm tra: Với mọi thiết bị user cần, phòng phải có ít nhất 1 cái tương ứng
        // Logic: "máy chiếu" match với "Máy chiếu Sony", "Projector"
        for (String req : requiredDevices) {
            boolean found = room.getFixedDevices().stream()
                    .anyMatch(dbDevice -> dbDevice.toLowerCase().contains(req.toLowerCase()));
            
            if (!found) return false; // Thiếu 1 cái là loại luôn
        }
        return true; // Có đủ hết
    }

}