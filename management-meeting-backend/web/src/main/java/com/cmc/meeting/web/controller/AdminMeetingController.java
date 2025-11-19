package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.request.ApprovalRequest; // DTO bạn đã tạo ở Bước 3
import com.cmc.meeting.application.port.service.MeetingService;
import com.cmc.meeting.domain.port.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/meetings")
@Tag(name = "Admin: Meeting Approval API", description = "API duyệt lịch họp")
@PreAuthorize("hasRole('ADMIN')") // Chỉ Admin được gọi
public class AdminMeetingController {

    private final MeetingService meetingService;
    private final UserRepository userRepository;

    public AdminMeetingController(MeetingService meetingService, UserRepository userRepository) {
        this.meetingService = meetingService;
        this.userRepository = userRepository;
    }

    /**
     * API Phê duyệt hoặc Từ chối cuộc họp
     */
    @PutMapping("/{id}/approval")
    @Operation(summary = "Phê duyệt (Approve) hoặc Từ chối (Reject) cuộc họp")
    public ResponseEntity<?> approveMeeting(
            @PathVariable Long id,
            @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // Lấy ID Admin đang thực hiện
        Long adminId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Admin not found"))
                .getId();

        // Gọi Service
        meetingService.processMeetingApproval(id, request.isApproved(), request.getReason(), adminId);
        
        String action = request.isApproved() ? "phê duyệt" : "từ chối";
        return ResponseEntity.ok("Đã " + action + " cuộc họp thành công.");
    }
}