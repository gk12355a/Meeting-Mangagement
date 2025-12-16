package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.report.CancelationReportDTO;
import com.cmc.meeting.application.dto.report.RoomUsageReportDTO;
import com.cmc.meeting.application.dto.report.VisitorReportDTO;
import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.application.port.service.ExcelExportService;
import com.cmc.meeting.application.port.service.MeetingService;
import com.cmc.meeting.application.port.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Report API", description = "API Thống kê & Báo cáo")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')") // <-- KHÓA TOÀN BỘ CONTROLLER
public class ReportController {

    private final ReportService reportService;
    private final ExcelExportService excelExportService;
    private final MeetingService meetingService;

    public ReportController(ReportService reportService, 
                            ExcelExportService excelExportService, MeetingService meetingService) {
        this.reportService = reportService;
        this.excelExportService = excelExportService; // BỔ SUNG
        this.meetingService = meetingService;
    }

    @GetMapping("/room-usage")
    @Operation(summary = "Báo cáo tần suất sử dụng phòng họp (US-22). Thêm ?format=excel để tải về.")
    public ResponseEntity<?> getRoomUsageReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String format) { // <-- Thêm param "format"

        // 1. Lấy dữ liệu (như cũ)
        List<RoomUsageReportDTO> report = reportService.getRoomUsageReport(from, to);

        // 2. Kiểm tra xem có muốn xuất Excel không
        if ("excel".equalsIgnoreCase(format)) {

            // 3. Gọi Service Excel
            ByteArrayInputStream excelFile = excelExportService.exportRoomUsageReport(report);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=BaoCaoSuDungPhong.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(excelFile));
        } else {
            // 4. Trả về JSON (như cũ)
            return ResponseEntity.ok(report);
        }
    }
    // BỔ SUNG: (US-23)
    @GetMapping("/cancelation-stats")
    @Operation(summary = "Thống kê lý do hủy họp (US-23). Thêm ?format=excel để tải về.")
    public ResponseEntity<?> getCancelationStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String format) { // <-- Thêm param "format"

        // 1. Lấy dữ liệu (như cũ)
        List<CancelationReportDTO> report = reportService.getCancelationReport(from, to);

        // 2. Kiểm tra xem có muốn xuất Excel không
        if ("excel".equalsIgnoreCase(format)) {

            // 3. Gọi Service Excel
            ByteArrayInputStream excelFile = excelExportService.exportCancelationReport(report);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=BaoCaoHuyHop.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(excelFile));
        } else {
            // 4. Trả về JSON (như cũ)
            return ResponseEntity.ok(report);
        }
    }
    // BỔ SUNG: (BS-31)
    @GetMapping("/visitors")
    @Operation(summary = "Báo cáo danh sách khách mời trong ngày (cho Lễ tân)")
    public ResponseEntity<List<VisitorReportDTO>> getVisitorReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<VisitorReportDTO> report = reportService.getVisitorReport(date);
        return ResponseEntity.ok(report);
    }
    @GetMapping("/all-meetings")
    public ResponseEntity<Page<MeetingDTO>> getAllMeetings(Pageable pageable) {
        Page<MeetingDTO> meetings = meetingService.getAllMeetings(pageable);
        return ResponseEntity.ok(meetings);
    }
}