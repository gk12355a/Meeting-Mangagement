package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.attachment.AttachmentDTO;
import com.cmc.meeting.application.port.service.AttachmentService;
import com.cmc.meeting.domain.port.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/attachments")
@Tag(name = "Attachment API", description = "API upload/xóa file đính kèm (BS-4.1)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()") // Chỉ cần đăng nhập
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final UserRepository userRepository;

    public AttachmentController(AttachmentService attachmentService, UserRepository userRepository) {
        this.attachmentService = attachmentService;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/upload/{meetingId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file đính kèm cho cuộc họp (chỉ người tổ chức)")
    public ResponseEntity<AttachmentDTO> uploadAttachment(
            @PathVariable Long meetingId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        Long currentUserId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User không tồn tại từ token"))
                .getId();

        AttachmentDTO dto = attachmentService.uploadAttachment(meetingId, currentUserId, file);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @DeleteMapping("/{attachmentId}")
    @Operation(summary = "Xóa file đính kèm (chỉ người tổ chức)")
    public ResponseEntity<?> deleteAttachment(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        Long currentUserId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User không tồn tại từ token"))
                .getId();

        attachmentService.deleteAttachment(attachmentId, currentUserId);
        return ResponseEntity.ok("Đã xóa file đính kèm.");
    }
}