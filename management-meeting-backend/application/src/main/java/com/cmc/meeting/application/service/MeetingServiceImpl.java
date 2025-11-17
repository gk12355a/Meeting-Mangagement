package com.cmc.meeting.application.service;

// Imports cho DTOs (Request & Response)
import com.cmc.meeting.application.dto.meeting.CheckInRequest;
import com.cmc.meeting.application.dto.meeting.MeetingCancelRequest;
import com.cmc.meeting.application.dto.meeting.MeetingResponseRequest;
import com.cmc.meeting.application.dto.request.MeetingCreationRequest;
import com.cmc.meeting.application.dto.request.MeetingUpdateRequest;
import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.application.dto.response.MeetingParticipantDTO;
import com.cmc.meeting.application.dto.recurrence.RecurrenceRuleDTO;
import com.cmc.meeting.application.dto.timeslot.TimeSlotDTO;

// Imports cho Ports (Hợp đồng)
import com.cmc.meeting.application.port.service.MeetingService;
import com.cmc.meeting.application.port.service.NotificationService;
import com.cmc.meeting.domain.port.repository.DeviceRepository;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.domain.port.repository.RoomRepository;
import com.cmc.meeting.domain.port.repository.UserRepository;

// Imports cho Domain (Lõi nghiệp vụ)
import com.cmc.meeting.domain.event.MeetingCreatedEvent;
import com.cmc.meeting.domain.exception.MeetingConflictException;
import com.cmc.meeting.domain.exception.PolicyViolationException;
import com.cmc.meeting.domain.model.*; // Import tất cả (Meeting, User, Room, Role, Device, Status...)

// Imports cho Java & Spring
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MeetingServiceImpl implements MeetingService {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(MeetingServiceImpl.class);

    // Repositories (Ports)
    private final MeetingRepository meetingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    // Services & Utilities
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final NotificationService notificationService;

    // Constructor Injection
    public MeetingServiceImpl(MeetingRepository meetingRepository,
            RoomRepository roomRepository,
            UserRepository userRepository,
            ModelMapper modelMapper,
            ApplicationEventPublisher eventPublisher,
            DeviceRepository deviceRepository, NotificationService notificationService) {
        this.meetingRepository = meetingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.eventPublisher = eventPublisher;
        this.deviceRepository = deviceRepository;
        this.notificationService = notificationService;
    }

    /**
     * (US-1, US-3, BS-20.1) Tạo cuộc họp (Đơn lẻ, Định kỳ, Đặt thay)
     */
    @Override
    public MeetingDTO createMeeting(MeetingCreationRequest request, Long currentUserId) {

        // --- 1. Lấy Dữ liệu chung ---
        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tạo (creator)"));

        // (BS-20.1) Xác định Người tổ chức (Organizer)
        User organizer;
        if (request.getOnBehalfOfUserId() != null) {
            // Đặt lịch thay
            // TODO: Kiểm tra xem 'creator' có quyền 'ROLE_SECRETARY' để đặt thay không
            organizer = userRepository.findById(request.getOnBehalfOfUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tổ chức (onBehalfOf)"));
        } else {
            // Tự đặt
            organizer = creator;
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng họp"));

        Set<User> participants = request.getParticipantIds().stream()
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tham dự với ID: " + id)))
                .collect(Collectors.toSet());

        Set<Device> devices = request.getDeviceIds().stream()
                .map(id -> deviceRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thiết bị với ID: " + id)))
                .collect(Collectors.toSet());
        Set<String> guestEmails = (request.getGuestEmails() != null) ? request.getGuestEmails() : new HashSet<>();

        // --- 2. Xử lý Lịch định kỳ ---
        if (request.getRecurrenceRule() == null) {
            // --- A. HỌP 1 LẦN ---
            checkAccessAndConflicts(room, organizer, participants, devices, 
                                    request.getStartTime(), request.getEndTime(), null); // <-- SỬA Ở ĐÂY
            
            return createSingleMeeting(request, room, creator, organizer,
                    participants, devices, guestEmails, null);

        } else {
            // --- B. HỌP ĐỊNH KỲ (US-3) ---
            String seriesId = UUID.randomUUID().toString();
            List<TimeSlotDTO> slots = calculateRecurrenceSlots(
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getRecurrenceRule());

            // Kiểm tra xung đột cho TẤT CẢ các slot
            for (TimeSlotDTO slot : slots) {
                checkAccessAndConflicts(room, organizer, participants, devices, slot.getStartTime(), slot.getEndTime(), null);
            }

            // Tạo hàng loạt
            List<MeetingDTO> createdMeetings = new ArrayList<>();
            for (TimeSlotDTO slot : slots) {
                MeetingCreationRequest singleRequest = modelMapper.map(request, MeetingCreationRequest.class);
                singleRequest.setStartTime(slot.getStartTime());
                singleRequest.setEndTime(slot.getEndTime());

                createdMeetings.add(
                        createSingleMeeting(singleRequest, room, creator, organizer, participants, devices, guestEmails,
                                seriesId));
            }
            return createdMeetings.get(0);
        }
    }

    /**
     * (US-2) Hủy lịch họp
     */
    @Override
    public void cancelMeeting(Long meetingId, MeetingCancelRequest request, Long currentUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        // CHỈ người tổ chức mới được hủy
        if (!meeting.getOrganizer().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Chỉ người tổ chức mới có quyền hủy cuộc họp này.");
        }

        meeting.cancelMeeting(request.getReason());
        Meeting savedMeeting = meetingRepository.save(meeting);

        // BỔ SUNG: TẠO THÔNG BÁO IN-APP
        String message = String.format(
                "Cuộc họp '%s' (lúc %s) đã bị hủy.",
                savedMeeting.getTitle(),
                savedMeeting.getStartTime().toLocalDate());
        // Gửi cho tất cả (trừ người hủy)
        savedMeeting.getParticipants().stream()
                .filter(p -> !p.getUser().getId().equals(currentUserId))
                .forEach(p -> notificationService.createNotification(
                        p.getUser(), message, savedMeeting));
    }

    /**
     * (US-2) Sửa lịch họp
     */
    @Override
    public MeetingDTO updateMeeting(Long meetingId, MeetingUpdateRequest request, Long currentUserId) {

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        // CHỈ người tổ chức mới được sửa
        if (!meeting.getOrganizer().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Chỉ người tổ chức mới có quyền sửa cuộc họp này.");
        }

        if (meeting.getStatus() == BookingStatus.CANCELLED) {
            throw new PolicyViolationException("Không thể sửa cuộc họp đã bị hủy.");
        }
        if (meeting.getStartTime().isBefore(LocalDateTime.now())) {
            throw new PolicyViolationException("Không thể sửa cuộc họp đã diễn ra.");
        }

        Room newRoom = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng họp"));

        Set<User> newParticipantUsers = request.getParticipantIds().stream()
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tham dự với ID: " + id)))
                .collect(Collectors.toSet());

        Set<Device> newDevices = request.getDeviceIds().stream()
                .map(id -> deviceRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thiết bị với ID: " + id)))
                .collect(Collectors.toSet());
        Set<String> newGuestEmails = (request.getGuestEmails() != null) ? request.getGuestEmails() : new HashSet<>();

        // Kiểm tra quyền và xung đột cho phòng/thời gian MỚI
        checkAccessAndConflicts(newRoom, meeting.getOrganizer(), newParticipantUsers, newDevices,
                request.getStartTime(), request.getEndTime(),
                meetingId // <-- THÊM THAM SỐ NÀY (ID của cuộc họp đang sửa)
        );

        // Chuyển đổi sang Set<MeetingParticipant>
        Set<MeetingParticipant> newParticipants = new HashSet<>();
        User organizer = meeting.getOrganizer();
        newParticipants.add(new MeetingParticipant(organizer, ParticipantStatus.ACCEPTED, null));
        newParticipantUsers.forEach(user -> {
            if (!user.getId().equals(organizer.getId())) {
                newParticipants.add(
                        new MeetingParticipant(user, ParticipantStatus.PENDING, UUID.randomUUID().toString()));
            }
        });

        // Cập nhật các trường
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setStartTime(request.getStartTime());
        meeting.setEndTime(request.getEndTime());
        meeting.setRoom(newRoom);
        meeting.setParticipants(newParticipants);
        meeting.setDevices(newDevices);
        meeting.setGuestEmails(newGuestEmails);

        Meeting updatedMeeting = meetingRepository.save(meeting);
        return convertMeetingToDTO(updatedMeeting);
    }

    /**
     * Lấy chi tiết cuộc họp
     */
    @Override
    @Transactional(readOnly = true)
    public MeetingDTO getMeetingById(Long meetingId, Long currentUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        // Kiểm tra quyền: Phải là người tổ chức hoặc người tham dự
        boolean isOrganizer = meeting.getOrganizer().getId().equals(currentUserId);
        boolean isParticipant = meeting.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(currentUserId));

        if (!isOrganizer && !isParticipant) {
            throw new PolicyViolationException("Bạn không có quyền xem chi tiết cuộc họp này.");
        }

        return convertMeetingToDTO(meeting);
    }

    /**
     * (US-6) Lấy danh sách cuộc họp của tôi
     */
    @Override
    @Transactional(readOnly = true)
    public List<MeetingDTO> getMyMeetings(Long currentUserId) {
        List<Meeting> meetings = meetingRepository.findAllByUserId(currentUserId);
        return meetings.stream()
                .map(this::convertMeetingToDTO)
                .collect(Collectors.toList());
    }

    /**
     * (BS-1.1) Phản hồi lời mời
     */
    @Override
    public void respondToInvitation(Long meetingId, MeetingResponseRequest request, Long currentUserId) {
        if (request.getStatus() == ParticipantStatus.PENDING) {
            throw new IllegalArgumentException("Không thể đổi trạng thái về PENDING.");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));

        meeting.respondToInvitation(currentUser, request.getStatus());
        Meeting savedMeeting = meetingRepository.save(meeting);
        
        // BỔ SUNG: TẠO THÔNG BÁO IN-APP
        // Gửi thông báo cho Người tổ chức (organizer)
        String message = String.format(
            "%s đã %s lời mời tham gia cuộc họp '%s'.",
            currentUser.getFullName(),
            request.getStatus() == ParticipantStatus.ACCEPTED ? "chấp nhận" : "từ chối",
            savedMeeting.getTitle()
        );
        
        // ==========================================================
        // SỬA LỖI: Gọi hàm KHÔNG có 'savedMeeting'
        // (Để tránh gửi meetingId cho thông báo Phản hồi)
        // ==========================================================
        notificationService.createNotification(
            savedMeeting.getOrganizer(), message
        );
    }

    /**
     * (US-27) Check-in
     */
    @Override
    public String checkIn(CheckInRequest request, Long currentUserId) {
        Meeting meeting = meetingRepository.findCheckInEligibleMeeting(
                currentUserId,
                request.getRoomId(),
                LocalDateTime.now())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy cuộc họp hợp lệ để check-in tại phòng này cho bạn."));

        meeting.checkIn();
        meetingRepository.save(meeting);

        return String.format("Check-in thành công cho cuộc họp: %s", meeting.getTitle());
    }

    /**
     * Phản hồi qua link (Không cần Token)
     */
    @Override
    public String respondByLink(String token, ParticipantStatus status) {
        if (status == ParticipantStatus.PENDING) {
            return "Trạng thái không hợp lệ.";
        }

        Meeting meeting = meetingRepository.findMeetingByParticipantToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Link phản hồi không hợp lệ hoặc đã hết hạn."));

        MeetingParticipant participant = meeting.getParticipants().stream()
                .filter(p -> token.equals(p.getResponseToken()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tham dự với token này."));

        participant.setStatus(status);
        participant.setResponseToken(null);
        meetingRepository.save(meeting);

        if (status == ParticipantStatus.ACCEPTED) {
            return "Cảm ơn! Phản hồi (Chấp nhận) của bạn đã được ghi lại.";
        } else {
            return "Phản hồi (Từ chối) của bạn đã được ghi lại.";
        }
    }

    /**
     * (US-3) Hủy chuỗi lịch định kỳ
     */
    @Override
    public void cancelMeetingSeries(String seriesId, MeetingCancelRequest request, Long currentUserId) {
        List<Meeting> meetingsInSeries = meetingRepository.findAllBySeriesId(seriesId);

        if (meetingsInSeries.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy chuỗi cuộc họp.");
        }

        // Kiểm tra quyền (chỉ cần check 1 cuộc)
        Meeting firstMeeting = meetingsInSeries.get(0);
        if (!firstMeeting.getOrganizer().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Chỉ người tổ chức mới có quyền hủy chuỗi họp này.");
        }

        log.info("Hủy chuỗi {}: Tìm thấy {} cuộc họp.", seriesId, meetingsInSeries.size());

        for (Meeting meeting : meetingsInSeries) {
            // Chỉ hủy các cuộc họp trong tương lai và chưa bị hủy
            if (meeting.getStartTime().isAfter(LocalDateTime.now()) &&
                    meeting.getStatus() == BookingStatus.CONFIRMED) {

                log.info("-> Đang hủy Meeting ID: {}", meeting.getId());
                meeting.cancelMeeting(request.getReason());
                meetingRepository.save(meeting);
            }
        }
    }

    // --- CÁC HÀM HELPER (Private) ---

    /**
     * HELPER 1: Logic tạo 1 cuộc họp
     */
    private MeetingDTO createSingleMeeting(MeetingCreationRequest request, Room room,
            User creator, User organizer,
            Set<User> participants, Set<Device> devices, Set<String> guestEmails, String seriesId) {

        Meeting newMeeting = new Meeting(
                request.getTitle(),
                request.getStartTime(),
                request.getEndTime(),
                room,
                creator,
                organizer,
                participants,
                devices,
                guestEmails,
                seriesId);

        Meeting savedMeeting = meetingRepository.save(newMeeting);
        eventPublisher.publishEvent(new MeetingCreatedEvent(savedMeeting.getId()));

        // BỔ SUNG: TẠO THÔNG BÁO IN-APP
        String message = String.format(
                "%s đã mời bạn tham gia cuộc họp: %s",
                creator.getFullName(),
                savedMeeting.getTitle());
        savedMeeting.getParticipants().stream()
                .filter(p -> p.getStatus() == ParticipantStatus.PENDING)
                .forEach(p -> notificationService.createNotification(
                        p.getUser(), message, savedMeeting));
        return convertMeetingToDTO(savedMeeting);
    }

    /**
     * HELPER 2: Logic kiểm tra Xung đột VÀ Quyền (US-21)
     */
    private void checkAccessAndConflicts(Room room, User organizer, Set<User> participants, Set<Device> devices,
            LocalDateTime startTime, LocalDateTime endTime, Long meetingIdToIgnore){

        if (room.getStatus() == RoomStatus.UNDER_MAINTENANCE) {
            throw new PolicyViolationException(
                    String.format("Phòng '%s' đang bảo trì, không thể đặt.", room.getName()));
        }
        // 1. KIỂM TRA QUYỀN ĐẶT PHÒNG (US-21)
        Set<Role> requiredRoles = room.getRequiredRoles();
        if (requiredRoles != null && !requiredRoles.isEmpty()) {
            Set<Role> userRoles = organizer.getRoles();
            boolean hasPermission = userRoles.stream().anyMatch(requiredRoles::contains);

            if (!hasPermission) {
                throw new PolicyViolationException(
                        String.format("Người tổ chức (%s) không có quyền đặt phòng '%s'",
                                organizer.getFullName(), room.getName()));
            }
        }

        // 2. KIỂM TRA XUNG ĐỘT (Phòng)
        if (meetingRepository.isRoomBusy(room.getId(), startTime, endTime, meetingIdToIgnore)) {
            throw new MeetingConflictException(
                    String.format("Phòng '%s' đã bị đặt vào lúc %s", room.getName(), startTime));
        }

        // 3. KIỂM TRA XUNG ĐỘT (Người)
        Set<Long> userIdsToCheck = participants.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        userIdsToCheck.add(organizer.getId()); // Thêm cả organizer vào danh sách check

        List<Meeting> conflictingUserMeetings = meetingRepository
                .findConflictingMeetingsForUsers(userIdsToCheck, startTime, endTime, meetingIdToIgnore);

        if (!conflictingUserMeetings.isEmpty()) {
            // Tìm xem ai là người bị trùng
            String conflictingUser = conflictingUserMeetings.get(0).getParticipants().stream()
                    .map(p -> p.getUser().getFullName())
                    .collect(Collectors.joining(", "));

            throw new MeetingConflictException(
                    String.format("Người tham dự (%s) bị trùng lịch vào lúc %s", conflictingUser, startTime));
        }
        Set<Long> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toSet());
        if (meetingRepository.isDeviceBusy(deviceIds, startTime, endTime, meetingIdToIgnore)) {
                throw new MeetingConflictException(
                        String.format("Một hoặc nhiều thiết bị bạn chọn đã bị đặt vào lúc %s", startTime));
            }
    }

    /**
     * HELPER 3: Thuật toán tính toán các slot định kỳ (US-3)
     */
    private List<TimeSlotDTO> calculateRecurrenceSlots(LocalDateTime firstStartTime,
            LocalDateTime firstEndTime,
            RecurrenceRuleDTO rule) {
        List<TimeSlotDTO> slots = new ArrayList<>();
        long durationMinutes = java.time.Duration.between(firstStartTime, firstEndTime).toMinutes();

        LocalDateTime currentStartTime = firstStartTime;

        while (!currentStartTime.toLocalDate().isAfter(rule.getRepeatUntil())) {

            LocalDateTime currentEndTime = currentStartTime.plusMinutes(durationMinutes);
            slots.add(new TimeSlotDTO(currentStartTime, currentEndTime));

            // Tính toán lần lặp tiếp theo
            switch (rule.getFrequency()) {
                case DAILY:
                    currentStartTime = currentStartTime.plusDays(rule.getInterval());
                    break;
                case WEEKLY:
                    currentStartTime = currentStartTime.plusWeeks(rule.getInterval());
                    break;
                case MONTHLY:
                    currentStartTime = currentStartTime.plusMonths(rule.getInterval());
                    break;
            }
        }
        return slots;
    }

    // BỔ SUNG: (BS-2.1)
    @Override
    public MeetingDTO updateMeetingSeries(String seriesId, MeetingCreationRequest request, Long currentUserId) {

        // 1. Lấy danh sách họp cũ
        List<Meeting> meetingsInSeries = meetingRepository.findAllBySeriesId(seriesId);
        if (meetingsInSeries.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy chuỗi cuộc họp.");
        }

        // 2. Kiểm tra quyền sở hữu (dùng cuộc họp đầu tiên)
        Meeting firstMeeting = meetingsInSeries.get(0);
        if (!firstMeeting.getOrganizer().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Chỉ người tổ chức mới có quyền sửa chuỗi họp này.");
        }

        // 3. Hủy tất cả các cuộc họp CHƯA DIỄN RA
        String reason = "Cuộc họp định kỳ đã được cập nhật hoặc thay đổi.";
        for (Meeting meeting : meetingsInSeries) {
            if (meeting.getStartTime().isAfter(LocalDateTime.now()) &&
                    meeting.getStatus() == BookingStatus.CONFIRMED) {

                log.info("-> (Update) Hủy Meeting ID: {}", meeting.getId());
                meeting.cancelMeeting(reason);
                meetingRepository.save(meeting);
            }
        }

        // 4. Tạo chuỗi mới (Tái sử dụng toàn bộ logic)
        // (createMeeting sẽ tự kiểm tra xung đột mới)
        log.info("-> (Update) Đang tạo chuỗi họp mới thay thế...");
        return this.createMeeting(request, currentUserId);
    }

    // CẬP NHẬT: (US-6)
    @Override
    @Transactional(readOnly = true)
    public Page<MeetingDTO> getMyMeetings(Long currentUserId, Pageable pageable) {
        Page<Meeting> meetings = meetingRepository.findAllByUserId(currentUserId, pageable);
        return meetings.map(this::convertMeetingToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MeetingDTO> getAllMeetings(Pageable pageable) {

        // 1. Gọi hàm mới từ Repository Port
        Page<Meeting> meetingsPage = meetingRepository.findAllMeetings(pageable);

        // 2. Ánh xạ (map) sang DTO
        // (Nếu bạn có hàm helper convertToDTO, hãy dùng nó)
        return meetingsPage.map(this::convertMeetingToDTO);
    }

    /**
     * Ánh xạ (map) từ Meeting (Domain) sang MeetingDTO
     * Sửa lỗi thiếu 'status' của 'participants'.
     */
    private MeetingDTO convertMeetingToDTO(Meeting meeting) {
        if (meeting == null) {
            return null;
        }

        // 1. Ánh xạ các trường đơn giản (id, title, room, devices, ...)
        // ModelMapper sẽ tự động map các trường lồng như RoomDTO, UserDTO, DeviceDTO
        MeetingDTO dto = modelMapper.map(meeting, MeetingDTO.class);

        // 2. Ánh xạ 'participants' thủ công (PHẦN SỬA LỖI)
        if (meeting.getParticipants() != null) {
            List<MeetingParticipantDTO> participantDTOs = meeting.getParticipants().stream()
                    .map(this::convertParticipantToDTO) // Gọi hàm helper con
                    .collect(Collectors.toList());
            dto.setParticipants(participantDTOs);
        }

        return dto;
    }

    /**
     * Helper con: Ánh xạ MeetingParticipant (Domain) sang MeetingParticipantDTO
     */
    private MeetingParticipantDTO convertParticipantToDTO(MeetingParticipant participant) {
        if (participant == null) {
            return null;
        }

        MeetingParticipantDTO dto = new MeetingParticipantDTO();
        dto.setStatus(participant.getStatus()); // <-- Lấy status

        // Lấy thông tin User từ đối tượng lồng bên trong
        if (participant.getUser() != null) {
            dto.setId(participant.getUser().getId());
            dto.setFullName(participant.getUser().getFullName());
        }

        return dto;
    }

}