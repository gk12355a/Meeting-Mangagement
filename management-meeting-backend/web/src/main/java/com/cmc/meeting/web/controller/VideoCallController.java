package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.response.VideoCallTokenResponse;
import com.cmc.meeting.application.port.service.StreamService;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.domain.port.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/video-call")
@Tag(name = "Video Call API", description = "API tích hợp Stream Video Chat")
@RequiredArgsConstructor
@Slf4j
public class VideoCallController {

    private final StreamService streamService;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;

    // Helper lấy thông tin user hiện tại từ Token đăng nhập
    private User getCurrentUser(Object principal) {
        String username;

        // In log để xem chính xác principal là cái gì
        log.info("DEBUG: Principal Type: {}", principal.getClass().getName());
        log.info("DEBUG: Principal Value: {}", principal.toString());

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else if (principal instanceof org.springframework.security.oauth2.jwt.Jwt) {
            // Nếu dùng thư viện OAuth2 Resource Server
            username = ((org.springframework.security.oauth2.jwt.Jwt) principal).getSubject();
        } else {
            // Fallback: Cố gắng ép kiểu về String, nhưng khả năng cao sẽ sai nếu là Object
            // lạ
            username = principal.toString();
        }

        log.info("DEBUG: Searching user with username: {}", username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("ERROR: User with username '{}' not found in DB!", username);
                    return new EntityNotFoundException("User not found");
                });
    }

    @GetMapping("/token")
    @Operation(summary = "Lấy Token để tham gia Video Call (Có kiểm tra quyền)")
    public ResponseEntity<?> getToken( // Đổi về ResponseEntity<?> để trả về lỗi text nếu cần
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = true) Long meetingId // <--- 2. Nhận meetingId từ Frontend
    ) {
        log.info("Requesting Video Token for Meeting ID: {}", meetingId);

        // 1. Lấy user hiện tại
        User user = getCurrentUser(principal);
        Long currentUserId = user.getId();

        // 2. Tìm cuộc họp
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp với ID: " + meetingId));

        // 3. KIỂM TRA QUYỀN (Security Check) 🚨
        boolean isOrganizer = meeting.getOrganizer().getId().equals(currentUserId);
        
        // Kiểm tra trong danh sách khách mời (Giả sử bạn có list participants)
        // Lưu ý: Cần map sang model User hoặc check ID trong list
        boolean isParticipant = meeting.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(currentUserId));

        // Nếu không phải chủ phòng VÀ không phải khách mời -> CHẶN
        if (!isOrganizer && !isParticipant) {
            log.warn("User {} cố gắng truy cập trái phép vào Meeting {}", currentUserId, meetingId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Bạn không có quyền tham gia cuộc họp này."));
        }

        // 4. Nếu hợp lệ -> Tạo Token (Code cũ)
        String streamUserId = String.valueOf(user.getId());
        String token = streamService.createToken(streamUserId);

        VideoCallTokenResponse.UserDetailDTO userDto = new VideoCallTokenResponse.UserDetailDTO(
                streamUserId, 
                user.getFullName(), 
                "https://ui-avatars.com/api/?name=" + user.getFullName().replace(" ", "+")
        );

        VideoCallTokenResponse response = new VideoCallTokenResponse(
                token, 
                streamService.getApiKey(), 
                userDto
        );

        return ResponseEntity.ok(response);
    }
    
    // Class phụ để trả lỗi JSON đơn giản
    record ErrorResponse(String error) {} 
}