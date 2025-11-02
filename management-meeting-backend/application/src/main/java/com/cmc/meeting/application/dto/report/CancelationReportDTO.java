package com.cmc.meeting.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelationReportDTO {
    private String reason; // Lý do hủy
    private long count;    // Số lần
}