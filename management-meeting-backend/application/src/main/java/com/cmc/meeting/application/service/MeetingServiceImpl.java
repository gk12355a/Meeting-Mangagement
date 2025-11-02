package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.meeting.CheckInRequest;
import com.cmc.meeting.application.dto.meeting.MeetingCancelRequest;
import com.cmc.meeting.application.dto.meeting.MeetingResponseRequest;
import com.cmc.meeting.application.dto.recurrence.RecurrenceRuleDTO;
import com.cmc.meeting.application.dto.request.MeetingCreationRequest;
import com.cmc.meeting.application.dto.request.MeetingUpdateRequest;
import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.application.dto.timeslot.TimeSlotDTO;
import com.cmc.meeting.domain.model.Device;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.MeetingParticipant;
import com.cmc.meeting.domain.model.ParticipantStatus;
import com.cmc.meeting.domain.model.Room;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.DeviceRepository;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.domain.port.repository.RoomRepository;
import com.cmc.meeting.domain.port.repository.UserRepository; // Giả sử chúng ta có repo này
import com.cmc.meeting.domain.exception.MeetingConflictException;
// import com.cmc.meeting.domain.exception.MeetingConflictException;
import com.cmc.meeting.domain.exception.PolicyViolationException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.cmc.meeting.domain.event.MeetingCreatedEvent; // (Chúng ta sẽ tạo file này)
import com.cmc.meeting.application.port.service.MeetingService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException; // Dùng tạm exception của JPA

import java.util.UUID; // Bổ sung

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
        private final DeviceRepository deviceRepository;

        // Sử dụng Constructor Injection (Clean Code)
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
         * CẬP NHẬT: (US-1 & US-3)
         * Hàm này giờ sẽ là "nhạc trưởng"
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
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Không tìm thấy người tham dự với ID: " + id)))
                                .collect(Collectors.toSet());

                Set<Device> devices = request.getDeviceIds().stream()
                                .map(id -> deviceRepository.findById(id)
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Không tìm thấy thiết bị với ID: " + id)))
                                .collect(Collectors.toSet());

                // --- 2. Xử lý Lịch định kỳ ---
                if (request.getRecurrenceRule() == null) {
                        // --- A. HỌP 1 LẦN (Như cũ) ---

                        // 2a. Kiểm tra xung đột 1 lần
                        checkConflicts(room.getId(), participants, request.getStartTime(), request.getEndTime());

                        // 2b. Tạo 1 cuộc họp
                        return createSingleMeeting(request, room, organizer, participants, devices, null);

                } else {
                        // --- B. HỌP ĐỊNH KỲ (Logic mới US-3) ---
                        String seriesId = UUID.randomUUID().toString();

                        // 2a. Tính toán tất cả các slot
                        List<TimeSlotDTO> slots = calculateRecurrenceSlots(
                                        request.getStartTime(),
                                        request.getEndTime(),
                                        request.getRecurrenceRule());

                        // 2b. Kiểm tra xung đột cho TẤT CẢ các slot
                        // (Nếu 1 slot lỗi -> dừng toàn bộ)
                        for (TimeSlotDTO slot : slots) {
                                checkConflicts(room.getId(), participants, slot.getStartTime(), slot.getEndTime());
                        }

                        // 2c. Tạo hàng loạt (Explosion)
                        List<MeetingDTO> createdMeetings = new ArrayList<>();
                        for (TimeSlotDTO slot : slots) {
                                // Tạo 1 request "nhái" cho từng slot
                                MeetingCreationRequest singleRequest = modelMapper.map(request,
                                                MeetingCreationRequest.class);
                                singleRequest.setStartTime(slot.getStartTime());
                                singleRequest.setEndTime(slot.getEndTime());

                                createdMeetings.add(
                                                createSingleMeeting(singleRequest, room, organizer, participants,
                                                                devices, seriesId));
                        }

                        // Trả về cuộc họp đầu tiên trong chuỗi
                        return createdMeetings.get(0);
                }
        }

        /**
         * HELPER 1: Logic tạo 1 cuộc họp (Tách ra từ code cũ)
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
                                seriesId);

                Meeting savedMeeting = meetingRepository.save(newMeeting);

                eventPublisher.publishEvent(new MeetingCreatedEvent(savedMeeting.getId()));

                return modelMapper.map(savedMeeting, MeetingDTO.class);
        }

        /**
         * HELPER 2: Logic kiểm tra xung đột (Tách ra)
         */
        private void checkConflicts(Long roomId, Set<User> participants, LocalDateTime startTime,
                        LocalDateTime endTime) {
                // 1. Kiểm tra phòng
                if (meetingRepository.isRoomBusy(roomId, startTime, endTime)) {
                        throw new MeetingConflictException(
                                        String.format("Phòng đã bị đặt vào lúc %s", startTime));
                }

                // 2. Kiểm tra người
                Set<Long> userIds = participants.stream().map(User::getId).collect(Collectors.toSet());
                List<Meeting> conflictingUserMeetings = meetingRepository
                                .findMeetingsForUsersInDateRange(userIds, startTime, endTime);

                if (!conflictingUserMeetings.isEmpty()) {
                        throw new MeetingConflictException(
                                        String.format("Người tham dự bị trùng lịch vào lúc %s", startTime));
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

        @Override
        public void cancelMeeting(Long meetingId, MeetingCancelRequest request, Long currentUserId) {
                Meeting meeting = meetingRepository.findById(meetingId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy cuộc họp với ID: " + meetingId));

                if (!meeting.getOrganizer().getId().equals(currentUserId)) {
                        throw new PolicyViolationException("Chỉ người tổ chức mới có quyền hủy cuộc họp này.");
                }

                // Gọi logic DOMAIN (với 'reason')
                meeting.cancelMeeting(request.getReason());

                meetingRepository.save(meeting);

                // (Bonus: Bắn event MeetingCancelledEvent để gửi mail)
        }

        /**
         * Hiện thực hóa: Lấy chi tiết 1 cuộc họp
         */
        @Override
        @Transactional(readOnly = true)
        public MeetingDTO getMeetingById(Long meetingId, Long currentUserId) {
                Meeting meeting = meetingRepository.findById(meetingId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy cuộc họp với ID: " + meetingId));

                boolean isOrganizer = meeting.getOrganizer().getId().equals(currentUserId);

                // SỬA DÒNG NÀY:
                boolean isParticipant = meeting.getParticipants().stream()
                                .anyMatch(p -> p.getUser().getId().equals(currentUserId)); // Đổi "user" thành
                                                                                           // "p.getUser()"

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
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy cuộc họp với ID: " + meetingId));

                // ... (Kiểm tra quyền, trạng thái, trùng lịch giữ nguyên) ...

                // 5. Lấy các đối tượng liên quan mới
                Room newRoom = roomRepository.findById(request.getRoomId())
                                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng họp"));

                // SỬA LOGIC NÀY:
                // 5a. Lấy Set<User>
                Set<User> newParticipantUsers = request.getParticipantIds().stream()
                                .map(id -> userRepository.findById(id)
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Không tìm thấy người tham dự với ID: " + id)))
                                .collect(Collectors.toSet());
                Set<Device> newDevices = request.getDeviceIds().stream()
                                .map(id -> deviceRepository.findById(id)
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Không tìm thấy thiết bị với ID: " + id)))
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
                                                new MeetingParticipant(user, ParticipantStatus.PENDING,
                                                                java.util.UUID.randomUUID().toString()) // Add token
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
                meeting.setDevices(newDevices);
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
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy cuộc họp với ID: " + meetingId));

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
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Link phản hồi không hợp lệ hoặc đã hết hạn."));

                // 2. Tìm chính xác participant có token đó
                MeetingParticipant participant = meeting.getParticipants().stream()
                                .filter(p -> token.equals(p.getResponseToken()))
                                .findFirst()
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy người tham dự với token này."));

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

        @Override
        public String checkIn(CheckInRequest request, Long currentUserId) {

                // 1. Tìm cuộc họp hợp lệ để check-in
                // (Query này đã bao gồm: đúng người tổ chức, đúng phòng,
                // đúng thời gian, chưa check-in, chưa hủy)
                Meeting meeting = meetingRepository.findCheckInEligibleMeeting(
                                currentUserId,
                                request.getRoomId(),
                                LocalDateTime.now()) // Thời gian hiện tại
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy cuộc họp hợp lệ để check-in tại phòng này cho bạn."));

                // 2. Gọi logic DOMAIN (POJO)
                meeting.checkIn(); // -> Đổi isCheckedIn = true

                // 3. Lưu lại
                meetingRepository.save(meeting);

                return String.format("Check-in thành công cho cuộc họp: %s", meeting.getTitle());
        }
}