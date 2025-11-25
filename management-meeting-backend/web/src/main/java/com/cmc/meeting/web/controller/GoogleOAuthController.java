package com.cmc.meeting.web.controller;

import com.cmc.meeting.infrastructure.persistence.jpa.adapter.*;
import com.cmc.meeting.domain.port.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/google")
public class GoogleOAuthController {

    private final GoogleAuthService googleAuthService;
    private final UserRepository userRepository;

    public GoogleOAuthController(GoogleAuthService googleAuthService, UserRepository userRepository) {
        this.googleAuthService = googleAuthService;
        this.userRepository = userRepository;
    }

    // 1. Trả về link đăng nhập Google
    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> authorize() {
        return ResponseEntity.ok(Collections.singletonMap("url", googleAuthService.getAuthorizationUrl()));
    }

    // 2. Nhận Code từ Frontend gửi lên
    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, String> payload, 
                                      @AuthenticationPrincipal UserDetails userDetails) {
        String code = payload.get("code");
        Long userId = userRepository.findByUsername(userDetails.getUsername()).get().getId();
        
        googleAuthService.exchangeAndSaveToken(code, userId);
        return ResponseEntity.ok("Liên kết thành công!");
    }
}