package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.meeting.MeetingResponseRequest;
import com.cmc.meeting.application.dto.request.MeetingCreationRequest;
import com.cmc.meeting.application.dto.request.MeetingUpdateRequest;
import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.MeetingParticipant;
import com.cmc.meeting.domain.model.ParticipantStatus;
import com.cmc.meeting.domain.model.Room;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.domain.port.repository.RoomRepository;
import com.cmc.meeting.domain.port.repository.UserRepository; // Giả sử chúng ta có repo này
// import com.cmc.meeting.domain.exception.MeetingConflictException;
import com.cmc.meeting.domain.exception.PolicyViolationException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.cmc.meeting.domain.event.MeetingCreatedEvent; // (Chúng ta sẽ tạo file này)
import com.cmc.meeting.application.port.service.MeetingService;
import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException; // Dùng tạm exception của JPA

@Service
@Transactional // Đảm bảo mọi thứ là 1 giao dịch (Yêu cầu 3.2)
public class MeetingServiceImpl implements MeetingService {

    // ---- Phụ thuộc vào các "Ports" (Interface) từ Domain ----
    // Chúng ta không phụ thuộc vào JPA hay MyBatiS
    private final MeetingRepository meetingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository; 

    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher; // Dùng để bắn event (Yêu cầu 4.1)

    // Sử dụng Constructor Injection (Clean Code)
    public MeetingServiceImpl(MeetingRepository meetingRepository,
                            RoomRepository roomRepository,
                            UserRepository userRepository,
                            ModelMapper modelMapper,
                            ApplicationEventPublisher eventPublisher) {
        this.meetingRepository = meetingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Hiện thực hóa User Story 1: Tạo lịch họp
     */
    @Override
    public MeetingDTO createMeeting(MeetingCreationRequest request, Long organizerId) {

        // --- 1. Validate Nghiệp vụ (Business Rule Validation) ---

        // (Chúng ta sẽ hiện thực logic này sau)
        // if (meetingRepository.isRoomBusy(request.getRoomId(), request.getStartTime(), request.getEndTime())) {
        //     throw new MeetingConflictException("Phòng đã bị đặt trong khung giờ này");
        // }

        // (Chúng ta cũng cần check lịch của người tham dự - US-5/BS-1.3)

        // --- 2. Lấy các đối tượng Domain (POJO) ---

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tổ chức"));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng họp"));

        Set<User> participants = request.getParticipantIds().stream()
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tham dự với ID: " + id)))
                .collect(Collectors.toSet());

        // --- 3. Tạo đối tượng Domain (Sử dụng logic trong model) ---
        Meeting newMeeting = new Meeting(
                request.getTitle(),
                request.getStartTime(),
                request.getEndTime(),
                room,
                organizer,
                participants
        );

        // --- 4. Lưu vào Database (thông qua Port) ---
        Meeting savedMeeting = meetingRepository.save(newMeeting);

        // --- 5. Bắn sự kiện (Event-Driven) - Yêu cầu 4.1 ---
        // Hệ thống sẽ gửi mail, đồng bộ calendar ở một luồng khác (@Async)
        // API sẽ trả về ngay lập tức cho user.
        eventPublisher.publishEvent(new MeetingCreatedEvent(savedMeeting.getId()));

        // --- 6. Map sang DTO để trả về cho user ---
        return modelMapper.map(savedMeeting, MeetingDTO.class);
    }
    @Override
    public void cancelMeeting(Long meetingId, Long currentUserId) {
        // 1. Tìm cuộc họp trong CSDL
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        // 2. KIỂM TRA QUYỀN (Business Rule): 
        // Chỉ người tổ chức (organizer) mới được hủy
        if (!meeting.getOrganizer().getId().equals(currentUserId)) {
            // Ném lỗi nghiệp vụ (chúng ta sẽ tạo Exception này)
            throw new PolicyViolationException("Chỉ người tổ chức mới có quyền hủy cuộc họp này.");
        }

        // 3. Gọi logic DOMAIN (POJO)
        // (POJO này chứa logic: không được hủy họp đã qua, không được hủy 2 lần)
        meeting.cancelMeeting(); // -> Đổi status thành CANCELLED

        // 4. Lưu lại trạng thái mới vào CSDL
        meetingRepository.save(meeting);
        
        // (Bonus: Chúng ta có thể bắn 1 event MeetingCancelledEvent
        // để gửi email thông báo cho người tham dự ở đây)
    }

    /**
     * Hiện thực hóa: Lấy chi tiết 1 cuộc họp
     */
    @Override
    @Transactional(readOnly = true)
    public MeetingDTO getMeetingById(Long meetingId, Long currentUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        boolean isOrganizer = meeting.getOrganizer().getId().equals(currentUserId);
        
        // SỬA DÒNG NÀY:
        boolean isParticipant = meeting.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(currentUserId)); // Đổi "user" thành "p.getUser()"

        if (!isOrganizer && !isParticipant) {
            throw new PolicyViolationException("Bạn không có quyền xem chi tiết cuộc họp này.");
        }

        return modelMapper.map(meeting, MeetingDTO.class);
    }

    /**
     * Hiện thực hóa User Story 6: Lấy danh sách cuộc họp của tôi
     */
    @Override
    @Transactional(readOnly = true) // Nghiệp vụ chỉ đọc
    public List<MeetingDTO> getMyMeetings(Long currentUserId) {
        // 1. Gọi query phức tạp (đã được định nghĩa ở Repository)
        List<Meeting> meetings = meetingRepository.findAllByUserId(currentUserId);

        // 2. Map List<Meeting> sang List<MeetingDTO>
        return meetings.stream()
                .map(meeting -> modelMapper.map(meeting, MeetingDTO.class))
                .collect(Collectors.toList());
    }
    /**
     * Hiện thực hóa User Story 2: Chỉnh sửa lịch họp
     */
    @Override
    public MeetingDTO updateMeeting(Long meetingId, MeetingUpdateRequest request, Long currentUserId) {
        
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        // ... (Kiểm tra quyền, trạng thái, trùng lịch giữ nguyên) ...
        
        // 5. Lấy các đối tượng liên quan mới
        Room newRoom = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng họp"));
                
        // SỬA LOGIC NÀY:
        // 5a. Lấy Set<User>
        Set<User> newParticipantUsers = request.getParticipantIds().stream()
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tham dự với ID: " + id)))
                .collect(Collectors.toSet());

        // 5b. Chuyển đổi sang Set<MeetingParticipant>
        Set<MeetingParticipant> newParticipants = new HashSet<>();
        User organizer = meeting.getOrganizer();
        
        // Add organizer
        newParticipants.add(new MeetingParticipant(organizer, ParticipantStatus.ACCEPTED, null)); // null token

        // Add những người khác
        newParticipantUsers.forEach(user -> {
            if (!user.getId().equals(organizer.getId())) {
                newParticipants.add(
                    new MeetingParticipant(user, ParticipantStatus.PENDING, UUID.randomUUID().toString()) // Tạo token mới
                );
            }
        });
        // KẾT THÚC SỬA LOGIC

        // 6. Cập nhật các trường
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setStartTime(request.getStartTime());
        meeting.setEndTime(request.getEndTime());
        meeting.setRoom(newRoom);
        meeting.setParticipants(newParticipants); // <-- Dòng này giờ đã đúng

        Meeting updatedMeeting = meetingRepository.save(meeting);
        return modelMapper.map(updatedMeeting, MeetingDTO.class);
    }
    @Override
    public void respondToInvitation(Long meetingId, MeetingResponseRequest request, Long currentUserId) {
        
        // 1. Kiểm tra trạng thái gửi lên
        if (request.getStatus() == ParticipantStatus.PENDING) {
            throw new IllegalArgumentException("Không thể đổi trạng thái về PENDING.");
        }

        // 2. Tìm cuộc họp
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        // 3. Lấy đối tượng User (người đang phản hồi)
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));
        
        // 4. Gọi logic DOMAIN (POJO)
        // (POJO này chứa logic kiểm tra xem user có trong danh sách mời không)
        meeting.respondToInvitation(currentUser, request.getStatus());

        // 5. Lưu lại trạng thái mới
        meetingRepository.save(meeting);
        
        // (Bonus: Bắn event MeetingRespondedEvent để thông báo cho organizer)
    }
    @Override
    public String respondByLink(String token, ParticipantStatus status) {
        if (status == ParticipantStatus.PENDING) {
            return "Trạng thái không hợp lệ.";
        }

        // 1. Tìm cuộc họp bằng token
        Meeting meeting = meetingRepository.findMeetingByParticipantToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Link phản hồi không hợp lệ hoặc đã hết hạn."));

        // 2. Tìm chính xác participant có token đó
        MeetingParticipant participant = meeting.getParticipants().stream()
                .filter(p -> token.equals(p.getResponseToken()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tham dự với token này."));

        // 3. Cập nhật trạng thái
        participant.setStatus(status);

        // 4. (Quan trọng) Xóa token để link chỉ dùng 1 lần
        participant.setResponseToken(null); 

        meetingRepository.save(meeting);

        if (status == ParticipantStatus.ACCEPTED) {
            return "Cảm ơn! Phản hồi (Chấp nhận) của bạn đã được ghi lại.";
        } else {
            return "Phản hồi (Từ chối) của bạn đã được ghi lại.";
        }
    }
}