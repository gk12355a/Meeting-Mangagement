package com.cmc.meeting.web.exception;

import com.cmc.meeting.domain.exception.MeetingConflictException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.cmc.meeting.domain.exception.PolicyViolationException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice // Đánh dấu đây là Global Handler
public class GlobalExceptionHandler {

    // 1. Xử lý lỗi Validation (từ @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST); // 400
    }

    // 2. Xử lý lỗi "Không tìm thấy" (vd: tìm phòng, user không có)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND); // 404
    }

    // 3. Xử lý lỗi "Trùng lịch" (Nghiệp vụ tùy chỉnh của chúng ta)
    @ExceptionHandler(MeetingConflictException.class)
    public ResponseEntity<Object> handleMeetingConflict(MeetingConflictException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT); // 409
    }

    // 4. Xử lý tất cả các lỗi khác (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex) {
        // (Nên log lỗi này ra)
        return new ResponseEntity<>(Map.of("error", "Lỗi máy chủ nội bộ: " + ex.getMessage()), 
                                    HttpStatus.INTERNAL_SERVER_ERROR); // 500
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex) {
        return new ResponseEntity<>(Map.of("error", "Sai tên đăng nhập hoặc mật khẩu"), 
                                    HttpStatus.UNAUTHORIZED); // 401
    }
    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<Object> handlePolicyViolation(PolicyViolationException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), 
                                    HttpStatus.FORBIDDEN); // 403
    }
}