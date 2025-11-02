package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.report.RoomUsageReportDTO;
import com.cmc.meeting.application.port.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Report API", description = "API Thống kê & Báo cáo (Chỉ Admin)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')") // <-- KHÓA TOÀN BỘ CONTROLLER
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/room-usage")
    @Operation(summary = "Báo cáo tần suất sử dụng phòng họp (US-22)")
    public ResponseEntity<List<RoomUsageReportDTO>> getRoomUsageReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<RoomUsageReportDTO> report = reportService.getRoomUsageReport(from, to);
        return ResponseEntity.ok(report);
    }
}