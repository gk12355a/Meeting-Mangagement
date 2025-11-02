package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.report.RoomUsageReportDTO;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    List<RoomUsageReportDTO> getRoomUsageReport(LocalDate fromDate, LocalDate toDate);
}