package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.auth.AuthResponse;
import com.cmc.meeting.application.dto.auth.LoginRequest;
import com.cmc.meeting.application.dto.auth.RegisterRequest;
import com.cmc.meeting.application.port.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication API", description = "API Đăng ký và Đăng nhập")
public class AuthController {

    // Inject interface của Application Layer
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * API Đăng nhập
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest) {
        
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(authResponse); // Trả về 200 OK + Token
    }

    /**
     * API Đăng ký
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest) {
        
        try {
            String message = authService.register(registerRequest);
            return ResponseEntity.ok(message); // Trả về 200 OK
        } catch (RuntimeException ex) {
            // Xử lý lỗi (vd: username đã tồn tại)
            // (GlobalExceptionHandler cũng sẽ bắt lỗi này)
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage()); // Trả về 400
        }
    }
}