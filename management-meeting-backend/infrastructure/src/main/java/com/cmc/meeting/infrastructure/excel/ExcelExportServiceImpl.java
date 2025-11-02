// 1. SỬA LẠI PACKAGE
package com.cmc.meeting.infrastructure.excel;

import com.cmc.meeting.application.dto.report.CancelationReportDTO;
// 2. BỔ SUNG CÁC IMPORT
import com.cmc.meeting.application.dto.report.RoomUsageReportDTO;
import com.cmc.meeting.application.port.service.ExcelExportService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service // Spring vẫn sẽ tìm thấy bean này
public class ExcelExportServiceImpl implements ExcelExportService { // Implement interface từ 'application'

    @Override
    public ByteArrayInputStream exportRoomUsageReport(List<RoomUsageReportDTO> reportData) {
        
        // Code này bâyS GIỜ sẽ hoạt động
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            
            Sheet sheet = workbook.createSheet("BaoCaoSuDungPhong");

            // Tạo hàng tiêu đề (Header)
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID Phòng", "Tên Phòng", "Số Lượt Đặt", "Tổng Số Giờ"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Đổ dữ liệu
            int rowIdx = 1;
            for (RoomUsageReportDTO report : reportData) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(report.getRoomId());
                row.createCell(1).setCellValue(report.getRoomName());
                row.createCell(2).setCellValue(report.getBookingCount());
                row.createCell(3).setCellValue(report.getTotalHoursBooked());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel: " + e.getMessage());
        }
    }
    @Override
    public ByteArrayInputStream exportCancelationReport(List<CancelationReportDTO> reportData) {

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {

            Sheet sheet = workbook.createSheet("BaoCaoHuyHop");

            // Tạo hàng tiêu đề (Header)
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Lý Do Hủy", "Số Lượt"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Đổ dữ liệu
            int rowIdx = 1;
            for (CancelationReportDTO report : reportData) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(report.getReason());
                row.createCell(1).setCellValue(report.getCount());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel: " + e.getMessage());
        }
    }
}