package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.report.CancelationReportDTO;
import com.cmc.meeting.application.dto.report.RoomUsageReportDTO;
import com.cmc.meeting.application.dto.report.VisitorReportDTO;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    List<RoomUsageReportDTO> getRoomUsageReport(LocalDate fromDate, LocalDate toDate);
    List<CancelationReportDTO> getCancelationReport(LocalDate fromDate, LocalDate toDate);
    List<VisitorReportDTO> getVisitorReport(LocalDate date);
}