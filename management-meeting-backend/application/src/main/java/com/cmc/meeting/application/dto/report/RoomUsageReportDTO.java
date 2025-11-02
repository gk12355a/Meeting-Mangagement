package com.cmc.meeting.application.dto.report;

import lombok.Data;

@Data
public class RoomUsageReportDTO {
    private Long roomId;
    private String roomName;
    private long bookingCount;  // Tổng số lượt đặt
    private double totalHoursBooked; // Tổng số giờ đã đặt

    // (Chúng ta có thể thêm 'occupancyRate' - Tỷ lệ lấp đầy - sau)
}