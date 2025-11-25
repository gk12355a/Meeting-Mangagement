package com.cmc.meeting.domain.model;

public enum BookingStatus {
    CONFIRMED, // Đã xác nhận
    CANCELLED, // Đã hủy
    PENDING, // Đang chờ (ví dụ: chờ phản hồi)
    PENDING_APPROVAL, // Chờ Admin duyệt
    REJECTED // Admin từ chối
}