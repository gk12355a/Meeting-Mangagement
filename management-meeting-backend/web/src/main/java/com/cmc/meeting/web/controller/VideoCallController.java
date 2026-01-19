package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.response.VideoCallTokenResponse;
import com.cmc.meeting.application.port.service.StreamService;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    @Operation(summary = "Lấy Token để tham gia Video Call")
    public ResponseEntity<VideoCallTokenResponse> getToken(@AuthenticationPrincipal Object principal) {
        User user = getCurrentUser(principal);
        String streamUserId = String.valueOf(user.getId());
        String token = streamService.createToken(streamUserId);

        // Tạo object response chuẩn
        VideoCallTokenResponse.UserDetailDTO userDto = new VideoCallTokenResponse.UserDetailDTO(
                streamUserId,
                user.getFullName(),
                "avatar-url...");

        VideoCallTokenResponse response = new VideoCallTokenResponse(
                token,
                streamService.getApiKey(),
                userDto);

        return ResponseEntity.ok(response);
    }
}