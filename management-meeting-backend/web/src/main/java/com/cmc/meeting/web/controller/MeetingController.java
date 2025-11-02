package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.meeting.CheckInRequest;
import com.cmc.meeting.application.dto.meeting.MeetingResponseRequest;
import com.cmc.meeting.application.dto.request.MeetingCreationRequest;
import com.cmc.meeting.application.dto.request.MeetingUpdateRequest;
import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.application.port.service.MeetingService;
import com.cmc.meeting.domain.model.ParticipantStatus;
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
import java.util.List;
@RestController
@RequestMapping("/api/v1/meetings")
@Tag(name = "Meeting API", description = "API quản lý lịch họp")
public class MeetingController {

    private final MeetingService meetingService;
    // BỔ SUNG: Chúng ta cần UserRepository để lấy User domain
    private final UserRepository userRepository;

    // CẬP NHẬT CONSTRUCTOR
    public MeetingController(MeetingService meetingService, UserRepository userRepository) {
        this.meetingService = meetingService;
        this.userRepository = userRepository;
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
    @DeleteMapping("/{id}") // Dùng phương thức DELETE
    @Operation(summary = "Hủy một lịch họp (chỉ người tổ chức)")
    public ResponseEntity<?> cancelMeeting(
            @PathVariable Long id, // Lấy ID từ URL (vd: /api/v1/meetings/1)
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 1. Lấy username từ token
        String username = userDetails.getUsername();

        // 2. Lấy ID user
        com.cmc.meeting.domain.model.User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy user từ token"));
        
        // 3. Gọi service
        meetingService.cancelMeeting(id, currentUser.getId());
        
        return ResponseEntity.ok("Đã hủy cuộc họp thành công."); // 200 OK
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
                "<html><body style='font-family: Arial; text-align: center; margin-top: 50px;'>" +
                "<h1>%s</h1>" +
                "<p>Bạn có thể đóng tab này.</p>" +
                "</body></html>", 
                message
            );
            return ResponseEntity.ok(htmlResponse);

        } catch (Exception e) {
            String htmlError = String.format(
                "<html><body style='font-family: Arial; text-align: center; margin-top: 50px;'>" +
                "<h1>Đã xảy ra lỗi</h1>" +
                "<p>%s</p>" +
                "</body></html>", 
                e.getMessage()
            );
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
}