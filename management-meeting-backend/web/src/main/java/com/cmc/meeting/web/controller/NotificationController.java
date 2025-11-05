package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.notification.NotificationDTO;
import com.cmc.meeting.application.port.service.NotificationService;
import com.cmc.meeting.domain.port.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification API", description = "API Thông báo In-App (US-16)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()") // Chỉ user đã đăng nhập
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo (có phân trang)")
    public ResponseEntity<Page<NotificationDTO>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {

        Long currentUserId = getUserId(userDetails);
        Page<NotificationDTO> page = notificationService.getMyNotifications(currentUserId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Lấy số lượng thông báo chưa đọc")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = getUserId(userDetails);
        Map<String, Long> count = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Đánh dấu 1 thông báo là đã đọc")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = getUserId(userDetails);
        NotificationDTO dto = notificationService.markAsRead(id, currentUserId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo là đã đọc")
    public ResponseEntity<?> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = getUserId(userDetails);
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok("Đã đánh dấu tất cả là đã đọc.");
    }

    // Helper
    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại từ token"))
                .getId();
    }
}