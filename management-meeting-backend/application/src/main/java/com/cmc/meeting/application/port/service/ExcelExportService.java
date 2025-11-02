package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.report.CancelationReportDTO;
import com.cmc.meeting.application.dto.report.RoomUsageReportDTO;
import java.io.ByteArrayInputStream;
import java.util.List;

public interface ExcelExportService {
    /**
     * Tạo file Excel từ dữ liệu Báo cáo Sử dụng phòng
     */
    ByteArrayInputStream exportRoomUsageReport(List<RoomUsageReportDTO> reportData);
    ByteArrayInputStream exportCancelationReport(List<CancelationReportDTO> reportData);
}