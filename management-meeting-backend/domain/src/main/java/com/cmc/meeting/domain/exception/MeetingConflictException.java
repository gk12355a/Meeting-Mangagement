package com.cmc.meeting.domain.exception;

// Exception nghiệp vụ tùy chỉnh cho lỗi trùng lịch
public class MeetingConflictException extends RuntimeException {
    public MeetingConflictException(String message) {
        super(message);
    }
}