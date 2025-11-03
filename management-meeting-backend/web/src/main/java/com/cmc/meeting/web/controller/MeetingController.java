package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.meeting.CheckInRequest;
import com.cmc.meeting.application.dto.meeting.MeetingCancelRequest;
import com.cmc.meeting.application.dto.meeting.MeetingResponseRequest;
import com.cmc.meeting.application.dto.request.MeetingCreationRequest;
import com.cmc.meeting.application.dto.request.MeetingUpdateRequest;
import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.application.port.service.MeetingService;
import com.cmc.meeting.domain.model.ParticipantStatus;
import com.cmc.meeting.domain.model.User;
// BỔ SUNG: Import 2 thư viện này
import com.cmc.meeting.domain.port.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// -------------------------
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cmc.meeting.application.dto.timeslot.TimeSlotDTO; // Bổ sung
import com.cmc.meeting.application.dto.timeslot.TimeSuggestionRequest; // Bổ sung
import com.cmc.meeting.application.port.service.TimeSuggestionService; // Bổ sung
import java.util.List; // Bổ sung

@RestController
@RequestMapping("/api/v1/meetings")
@Tag(name = "Meeting API", description = "API quản lý lịch họp")
public class MeetingController {

        private final MeetingService meetingService;
        // BỔ SUNG: Chúng ta cần UserRepository để lấy User domain
        private final UserRepository userRepository;
        private final TimeSuggestionService timeSuggestionService;

        // CẬP NHẬT CONSTRUCTOR
        public MeetingController(MeetingService meetingService, UserRepository userRepository,
                        TimeSuggestionService timeSuggestionService) {
                this.meetingService = meetingService;
                this.userRepository = userRepository;
                this.timeSuggestionService = timeSuggestionService;
        }

        @PostMapping
        @Operation(summary = "Tạo một lịch họp mới (US-1)")
        public ResponseEntity<MeetingDTO> createMeeting(
                        @Valid @RequestBody MeetingCreationRequest request,
                        // BỔ SUNG: Tự động lấy user đã xác thực
                        @AuthenticationPrincipal UserDetails userDetails) {

                // 1. Lấy username từ token (đã được xác thực)
                String username = userDetails.getUsername();

                // 2. Dùng username để lấy đối tượng User (domain)
                // (Vì service của chúng ta cần Long userId)
                com.cmc.meeting.domain.model.User currentUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy user từ token"));

                // 3. Gọi service với ID user thật
                MeetingDTO createdMeeting = meetingService.createMeeting(request, currentUser.getId());

                return new ResponseEntity<>(createdMeeting, HttpStatus.CREATED); // 201
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Hủy một lịch họp (chỉ người tổ chức) - Bắt buộc lý do")
        public ResponseEntity<?> cancelMeeting(
                        @PathVariable Long id,
                        @Valid @RequestBody MeetingCancelRequest request, // <-- THÊM
                        @AuthenticationPrincipal UserDetails userDetails) {

                com.cmc.meeting.domain.model.User currentUser = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy user từ token"));

                // Gọi service (đã cập nhật)
                meetingService.cancelMeeting(id, request, currentUser.getId());

                return ResponseEntity.ok("Đã hủy cuộc họp thành công.");
        }

        /**
         * API Lấy chi tiết một cuộc họp (US ẩn)
         * Chỉ người tổ chức hoặc người tham dự mới được xem.
         */
        @GetMapping("/{id}")
        @Operation(summary = "Lấy chi tiết một cuộc họp (chỉ người tham gia/tổ chức)")
        public ResponseEntity<MeetingDTO> getMeetingById(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserDetails userDetails) {

                // 1. Lấy ID user từ token
                com.cmc.meeting.domain.model.User currentUser = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy user từ token"));

                // 2. Gọi service
                MeetingDTO meetingDTO = meetingService.getMeetingById(id, currentUser.getId());

                return ResponseEntity.ok(meetingDTO); // 200 OK
        }

        /**
         * API Lấy danh sách các cuộc họp của tôi (US-6)
         * (Các cuộc họp tôi tổ chức HOẶC được mời)
         */
        @GetMapping("/my-meetings")
        @Operation(summary = "Lấy danh sách các cuộc họp của tôi (tổ chức hoặc tham dự)")
        public ResponseEntity<List<MeetingDTO>> getMyMeetings(
                        @AuthenticationPrincipal UserDetails userDetails) {

                // 1. Lấy ID user từ token
                com.cmc.meeting.domain.model.User currentUser = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy user từ token"));

                // 2. Gọi service
                List<MeetingDTO> meetings = meetingService.getMyMeetings(currentUser.getId());

                return ResponseEntity.ok(meetings); // 200 OK
        }

        /**
         * API Cập nhật/Sửa một lịch họp (US-2)
         */
        @PutMapping("/{id}") // Dùng phương thức PUT
        @Operation(summary = "Cập nhật một lịch họp (chỉ người tổ chức)")
        public ResponseEntity<MeetingDTO> updateMeeting(
                        @PathVariable Long id, // Lấy ID từ URL
                        @Valid @RequestBody MeetingUpdateRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                // 1. Lấy ID user từ token
                com.cmc.meeting.domain.model.User currentUser = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy user từ token"));

                // 2. Gọi service
                MeetingDTO updatedMeeting = meetingService.updateMeeting(id, request, currentUser.getId());

                return ResponseEntity.ok(updatedMeeting); // 200 OK
        }

        @PostMapping("/{id}/respond") // Dùng phương thức POST
        @Operation(summary = "Chấp nhận (ACCEPT) hoặc Từ chối (DECLINE) một lời mời họp")
        public ResponseEntity<?> respondToInvitation(
                        @PathVariable Long id, // Lấy ID cuộc họp từ URL
                        @Valid @RequestBody MeetingResponseRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                // 1. Lấy ID user từ token
                com.cmc.meeting.domain.model.User currentUser = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy user từ token"));

                // 2. Gọi service
                meetingService.respondToInvitation(id, request, currentUser.getId());

                return ResponseEntity.ok("Đã phản hồi lời mời thành công."); // 200 OK
        }

        @GetMapping(value = "/respond-by-link", produces = "text/html;charset=UTF-8")
        @Operation(summary = "Phản hồi lời mời họp qua link email (Không cần đăng nhập)")
        public ResponseEntity<String> respondByLink(
                        @RequestParam String token,
                        @RequestParam ParticipantStatus status) {

                try {
                        String message = meetingService.respondByLink(token, status);

                        // Trả về 1 trang HTML đơn giản
                        String htmlResponse = String.format(
                                        "<html><body style='font-family: Arial; text-align: center; margin-top: 50px;'>"
                                                        +
                                                        "<h1>%s</h1>" +
                                                        "<p>Bạn có thể đóng tab này.</p>" +
                                                        "</body></html>",
                                        message);
                        return ResponseEntity.ok(htmlResponse);

                } catch (Exception e) {
                        String htmlError = String.format(
                                        "<html><body style='font-family: Arial; text-align: center; margin-top: 50px;'>"
                                                        +
                                                        "<h1>Đã xảy ra lỗi</h1>" +
                                                        "<p>%s</p>" +
                                                        "</body></html>",
                                        e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(htmlError);
                }
        }

        @PostMapping("/check-in")
        @Operation(summary = "Check-in vào một cuộc họp (chỉ người tổ chức)")
        public ResponseEntity<?> checkInToMeeting(
                        @Valid @RequestBody CheckInRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                // 1. Lấy ID user từ token
                com.cmc.meeting.domain.model.User currentUser = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy user từ token"));

                // 2. Gọi service
                String message = meetingService.checkIn(request, currentUser.getId());

                return ResponseEntity.ok(message); // 200 OK
        }

        /**
         * API Gợi ý thời gian họp (US-5)
         */
        @PostMapping("/suggest-time")
        @Operation(summary = "Gợi ý các khung thời gian họp còn trống cho một nhóm người")
        public ResponseEntity<List<TimeSlotDTO>> suggestTime(
                        @Valid @RequestBody TimeSuggestionRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                // (Chúng ta có thể thêm logic kiểm tra xem người gọi
                // có trong danh sách participantIds không)

                List<TimeSlotDTO> suggestions = timeSuggestionService.suggestTime(request);
                return ResponseEntity.ok(suggestions);
        }

        /**
         * API Hủy toàn bộ chuỗi lịch định kỳ (US-3)
         */
        @DeleteMapping("/series/{seriesId}")
        @Operation(summary = "Hủy toàn bộ chuỗi lịch định kỳ (chỉ người tổ chức)")
        public ResponseEntity<?> cancelMeetingSeries(
                        @PathVariable String seriesId, // Lấy ID chuỗi từ URL
                        @Valid @RequestBody MeetingCancelRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                com.cmc.meeting.domain.model.User currentUser = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy user từ token"));

                meetingService.cancelMeetingSeries(seriesId, request, currentUser.getId());

                return ResponseEntity.ok("Đã hủy các cuộc họp (chưa diễn ra) trong chuỗi thành công.");
        }

        /**
         * API Cập nhật toàn bộ chuỗi lịch định kỳ (BS-2.1)
         * (Bằng cách hủy cũ, tạo mới)
         */
        @PutMapping("/series/{seriesId}")
        @Operation(summary = "Cập nhật toàn bộ chuỗi lịch định kỳ (chỉ người tổ chức)")
        public ResponseEntity<MeetingDTO> updateMeetingSeries(
                        @PathVariable String seriesId,
                        @Valid @RequestBody MeetingCreationRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                User currentUser = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy user từ token"));

                // (request phải chứa RecurrenceRule mới)
                if (request.getRecurrenceRule() == null) {
                        throw new IllegalArgumentException("Cập nhật chuỗi phải bao gồm RecurrenceRule mới.");
                }

                MeetingDTO firstMeeting = meetingService.updateMeetingSeries(
                                seriesId, request, currentUser.getId());

                return ResponseEntity.ok(firstMeeting);
        }

}