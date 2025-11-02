package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.meeting.CheckInRequest;
import com.cmc.meeting.application.dto.meeting.MeetingCancelRequest;
import com.cmc.meeting.application.dto.meeting.MeetingResponseRequest;
import com.cmc.meeting.application.dto.request.MeetingCreationRequest;
import com.cmc.meeting.application.dto.request.MeetingUpdateRequest;
import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.application.dto.recurrence.RecurrenceRuleDTO;
import com.cmc.meeting.application.dto.timeslot.TimeSlotDTO;
import com.cmc.meeting.application.port.service.MeetingService;
import com.cmc.meeting.domain.event.MeetingCreatedEvent;
import com.cmc.meeting.domain.exception.MeetingConflictException;
import com.cmc.meeting.domain.exception.PolicyViolationException;
import com.cmc.meeting.domain.model.*; // Import tất cả model (gồm cả Role)
import com.cmc.meeting.domain.port.repository.DeviceRepository;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.domain.port.repository.RoomRepository;
import com.cmc.meeting.domain.port.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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

    private static final Logger log = LoggerFactory.getLogger(MeetingServiceImpl.class);

    private final MeetingRepository meetingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final DeviceRepository deviceRepository;

    public MeetingServiceImpl(MeetingRepository meetingRepository,
                            RoomRepository roomRepository,
                            UserRepository userRepository,
                            ModelMapper modelMapper,
                            ApplicationEventPublisher eventPublisher,
                            DeviceRepository deviceRepository) {
        this.meetingRepository = meetingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.eventPublisher = eventPublisher;
        this.deviceRepository = deviceRepository;
    }


    /**
     * (US-1 & US-3)
     */
    @Override
    public MeetingDTO createMeeting(MeetingCreationRequest request, Long organizerId) {
        
        // --- 1. Lấy Dữ liệu chung ---
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tổ chức"));
        
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

        // --- 2. Xử lý Lịch định kỳ ---
        if (request.getRecurrenceRule() == null) {
            // --- A. HỌP 1 LẦN ---
            checkAccessAndConflicts(room, organizer, participants, request.getStartTime(), request.getEndTime());
            return createSingleMeeting(request, room, organizer, participants, devices, null);
            
        } else {
            // --- B. HỌP ĐỊNH KỲ ---
            String seriesId = UUID.randomUUID().toString();
            List<TimeSlotDTO> slots = calculateRecurrenceSlots(
                    request.getStartTime(), 
                    request.getEndTime(), 
                    request.getRecurrenceRule()
            );

            for (TimeSlotDTO slot : slots) {
                checkAccessAndConflicts(room, organizer, participants, slot.getStartTime(), slot.getEndTime());
            }

            List<MeetingDTO> createdMeetings = new ArrayList<>();
            for (TimeSlotDTO slot : slots) {
                MeetingCreationRequest singleRequest = modelMapper.map(request, MeetingCreationRequest.class);
                singleRequest.setStartTime(slot.getStartTime());
                singleRequest.setEndTime(slot.getEndTime());
                
                createdMeetings.add(
                    createSingleMeeting(singleRequest, room, organizer, participants, devices, seriesId)
                );
            }
            
            return createdMeetings.get(0);
        }
    }

    /**
     * HELPER 1: Logic tạo 1 cuộc họp
     */
    private MeetingDTO createSingleMeeting(MeetingCreationRequest request, Room room, User organizer, 
                                           Set<User> participants, Set<Device> devices, String seriesId) {

        Meeting newMeeting = new Meeting(
                request.getTitle(),
                request.getStartTime(),
                request.getEndTime(),
                room,
                organizer,
                participants,
                devices,
                seriesId
        );
        
        Meeting savedMeeting = meetingRepository.save(newMeeting);
        eventPublisher.publishEvent(new MeetingCreatedEvent(savedMeeting.getId()));
        return modelMapper.map(savedMeeting, MeetingDTO.class);
    }

    /**
     * HELPER 2 (ĐÃ SỬA): Logic kiểm tra Xung đột VÀ Quyền (US-21)
     */
    private void checkAccessAndConflicts(Room room, User organizer, Set<User> participants, 
                                         LocalDateTime startTime, LocalDateTime endTime) {
        
        // 1. KIỂM TRA QUYỀN ĐẶT PHÒNG (US-21)
        Set<Role> requiredRoles = room.getRequiredRoles();
        if (requiredRoles != null && !requiredRoles.isEmpty()) {
            Set<Role> userRoles = organizer.getRoles();
            boolean hasPermission = userRoles.stream().anyMatch(requiredRoles::contains);
            
            if (!hasPermission) {
                throw new PolicyViolationException(
                    String.format("Bạn không có quyền (ví dụ: ROLE_VIP) để đặt phòng '%s'", room.getName())
                );
            }
        }
        
        // 2. KIỂM TRA XUNG ĐỘT (Code cũ)
        // Kiểm tra phòng
        if (meetingRepository.isRoomBusy(room.getId(), startTime, endTime)) {
            throw new MeetingConflictException(
                String.format("Phòng đã bị đặt vào lúc %s", startTime)
            );
        }
        
        // Kiểm tra người
        Set<Long> userIds = participants.stream().map(User::getId).collect(Collectors.toSet());
        List<Meeting> conflictingUserMeetings = meetingRepository
                .findMeetingsForUsersInDateRange(userIds, startTime, endTime); // <-- Sửa lỗi 2
        
        if (!conflictingUserMeetings.isEmpty()) {
            throw new MeetingConflictException(
                String.format("Người tham dự bị trùng lịch vào lúc %s", startTime) // <-- Sửa lỗi 3
            );
        }
    }

    /**
     * HELPER 3: Thuật toán tính toán các slot định kỳ
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

    // --- CÁC NGHIỆP VỤ KHÁC ---
    
    @Override
    public void cancelMeeting(Long meetingId, MeetingCancelRequest request, Long currentUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        if (!meeting.getOrganizer().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Chỉ người tổ chức mới có quyền hủy cuộc họp này.");
        }

        meeting.cancelMeeting(request.getReason()); 
        meetingRepository.save(meeting);
    }

    @Override
    public MeetingDTO updateMeeting(Long meetingId, MeetingUpdateRequest request, Long currentUserId) {
        
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

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
                
        // (Kiểm tra xung đột cho phòng mới)
        if (meetingRepository.isRoomBusy(request.getRoomId(), request.getStartTime(), request.getEndTime()) &&
            !meeting.getRoom().getId().equals(request.getRoomId())) {
             throw new MeetingConflictException("Phòng đã bị đặt trong khung giờ này.");
        }

        Set<User> newParticipantUsers = request.getParticipantIds().stream()
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tham dự với ID: " + id)))
                .collect(Collectors.toSet());

        Set<MeetingParticipant> newParticipants = new HashSet<>();
        User organizer = meeting.getOrganizer();
        
        newParticipants.add(new MeetingParticipant(organizer, ParticipantStatus.ACCEPTED, null));

        newParticipantUsers.forEach(user -> {
            if (!user.getId().equals(organizer.getId())) {
                newParticipants.add(
                    new MeetingParticipant(user, ParticipantStatus.PENDING, java.util.UUID.randomUUID().toString())
                );
            }
        });

        Set<Device> newDevices = request.getDeviceIds().stream()
                .map(id -> deviceRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thiết bị với ID: " + id)))
                .collect(Collectors.toSet());

        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setStartTime(request.getStartTime());
        meeting.setEndTime(request.getEndTime());
        meeting.setRoom(newRoom);
        meeting.setParticipants(newParticipants);
        meeting.setDevices(newDevices);

        Meeting updatedMeeting = meetingRepository.save(meeting);
        return modelMapper.map(updatedMeeting, MeetingDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public MeetingDTO getMeetingById(Long meetingId, Long currentUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        boolean isOrganizer = meeting.getOrganizer().getId().equals(currentUserId);
        
        boolean isParticipant = meeting.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(currentUserId)); 

        if (!isOrganizer && !isParticipant) {
            throw new PolicyViolationException("Bạn không có quyền xem chi tiết cuộc họp này.");
        }

        return modelMapper.map(meeting, MeetingDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingDTO> getMyMeetings(Long currentUserId) {
        List<Meeting> meetings = meetingRepository.findAllByUserId(currentUserId);
        return meetings.stream()
                .map(meeting -> modelMapper.map(meeting, MeetingDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void respondToInvitation(Long meetingId, MeetingResponseRequest request, Long currentUserId) {
        if (request.getStatus() == ParticipantStatus.PENDING) {
            throw new IllegalArgumentException("Không thể đổi trạng thái về PENDING.");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId)); // <-- ĐÃ XÓA "d"

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));
        
        meeting.respondToInvitation(currentUser, request.getStatus());
        meetingRepository.save(meeting);
    }

    @Override
    public String checkIn(CheckInRequest request, Long currentUserId) {
        Meeting meeting = meetingRepository.findCheckInEligibleMeeting(
                        currentUserId, 
                        request.getRoomId(), 
                        LocalDateTime.now())
                    .orElseThrow(() -> 
                            new EntityNotFoundException(
                                "Không tìm thấy cuộc họp hợp lệ để check-in tại phòng này cho bạn."
                            ));

        meeting.checkIn();
        meetingRepository.save(meeting);
        
        return String.format("Check-in thành công cho cuộc họp: %s", meeting.getTitle());
    }

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

    @Override
    public void cancelMeetingSeries(String seriesId, MeetingCancelRequest request, Long currentUserId) {
        List<Meeting> meetingsInSeries = meetingRepository.findAllBySeriesId(seriesId);
        
        if (meetingsInSeries.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy chuỗi cuộc họp.");
        }

        Meeting firstMeeting = meetingsInSeries.get(0);
        if (!firstMeeting.getOrganizer().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Chỉ người tổ chức mới có quyền hủy chuỗi họp này.");
        }

        log.info("Hủy chuỗi {}: Tìm thấy {} cuộc họp.", seriesId, meetingsInSeries.size());
        
        for (Meeting meeting : meetingsInSeries) {
            if (meeting.getStartTime().isAfter(LocalDateTime.now()) && 
                meeting.getStatus() == BookingStatus.CONFIRMED) {
                
                log.info("-> Đang hủy Meeting ID: {}", meeting.getId());
                meeting.cancelMeeting(request.getReason());
                meetingRepository.save(meeting);
            }
        }
    }
}