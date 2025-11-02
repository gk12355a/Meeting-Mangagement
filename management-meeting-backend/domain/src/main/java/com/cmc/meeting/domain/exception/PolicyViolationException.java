package com.cmc.meeting.domain.exception;

// Lỗi vi phạm chính sách/quyền hạn nghiệp vụ
public class PolicyViolationException extends RuntimeException {
    public PolicyViolationException(String message) {
        super(message);
    }
}