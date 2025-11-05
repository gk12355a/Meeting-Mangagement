package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.auth.AuthResponse;
import com.cmc.meeting.application.dto.auth.ChangePasswordRequest;
import com.cmc.meeting.application.dto.auth.ForgotPasswordRequest;
import com.cmc.meeting.application.dto.auth.LoginRequest;
import com.cmc.meeting.application.dto.auth.RegisterRequest;
import com.cmc.meeting.application.dto.auth.ResetPasswordRequest;
import com.cmc.meeting.application.port.service.AuthService;
import com.cmc.meeting.domain.port.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
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
    /**
     * API Yêu cầu link Quên mật khẩu (BS-5.1)
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu link đặt lại mật khẩu qua email")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        
        String message = authService.forgotPassword(request);
        // Luôn trả về 200 OK (vì lý do bảo mật)
        return ResponseEntity.ok(message); 
    }

    /**
     * API Hoàn tất Đặt lại mật khẩu (BS-5.3)
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Hoàn tất đặt lại mật khẩu (dùng token từ email)")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        
        try {
            String message = authService.resetPassword(request);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            // Bắt lỗi (vd: Token hết hạn, Token không tồn tại)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PostMapping("/change-password")
    @Operation(summary = "Tự đổi mật khẩu (khi đã đăng nhập)")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        // Lấy ID user từ token
        Long currentUserId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại từ token"))
                .getId();
        
        authService.changePassword(currentUserId, request);
        return ResponseEntity.ok("Đổi mật khẩu thành công.");
    }
}