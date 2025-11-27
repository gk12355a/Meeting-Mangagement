package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.port.service.ChatbotService;
import com.cmc.meeting.application.dto.chat.ChatRequest;
import com.cmc.meeting.application.dto.chat.ChatResponse;
// Import lớp UserDetails tùy chỉnh của bạn
import com.cmc.meeting.web.security.CustomUserDetailsService.CustomUserDetails; 

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/query")
    public ResponseEntity<ChatResponse> handleChatQuery(
            @RequestBody ChatRequest request,
            Authentication authentication) {
        
        // 1. Lấy thông tin người dùng đã đăng nhập từ Spring Security
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId(); 

        // 2. Gọi service ở tầng application
        ChatResponse response = chatbotService.processQuery(request.getQuery(), request.getHistory(), currentUserId);
        
        return ResponseEntity.ok(response);
    }
}