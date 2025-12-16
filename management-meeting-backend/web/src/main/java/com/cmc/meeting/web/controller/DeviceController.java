package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.device.DeviceDTO;
import com.cmc.meeting.application.dto.device.DeviceRequest;
import com.cmc.meeting.application.dto.response.BookedSlotDTO;
import com.cmc.meeting.application.port.service.DeviceService;
import com.cmc.meeting.application.port.service.MeetingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Device API", description = "API Quản lý thiết bị (Admin) và Tra cứu (User)")
@SecurityRequirement(name = "bearerAuth")
// @PreAuthorize("hasRole('ADMIN')") // <-- BẢO VỆ TOÀN BỘ CONTROLLER NÀY
public class DeviceController {

    private final DeviceService deviceService;
    private final MeetingService meetingService;
    public DeviceController(DeviceService deviceService, MeetingService meetingService) {
        this.deviceService = deviceService;
        this.meetingService = meetingService;
    }

    /**
     * API Lấy danh sách thiết bị (US-13)
     * Dành cho tất cả user đã đăng nhập
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả thiết bị (Cho tất cả user)")
    // BỔ SUNG: Cho phép USER
    @PreAuthorize("isAuthenticated()") 
    public ResponseEntity<List<DeviceDTO>> getAllDevices() {
        List<DeviceDTO> devices = deviceService.getAllDevices();
        return ResponseEntity.ok(devices);
    }

    @PostMapping
    @Operation(summary = "")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceDTO> createDevice(@Valid @RequestBody DeviceRequest request) {
        DeviceDTO createdDevice = deviceService.createDevice(request);
        return new ResponseEntity<>(createdDevice, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin thiết bị (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable Long id,
                                                @Valid @RequestBody DeviceRequest request) {
        DeviceDTO updatedDevice = deviceService.updateDevice(id, request);
        return ResponseEntity.ok(updatedDevice);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa một thiết bị (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.ok("Đã xóa thiết bị thành công.");
    }

    @GetMapping("/available")
    public ResponseEntity<List<DeviceDTO>> getAvailableDevices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        List<DeviceDTO> availableDevices = deviceService.findAvailableDevices(startTime, endTime);
        return ResponseEntity.ok(availableDevices);
    }
    @GetMapping("/{id}/meetings")
    @Operation(summary = "Lấy lịch sử dụng (lịch bận) của một thiết bị")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BookedSlotDTO>> getDeviceSchedule(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        List<BookedSlotDTO> schedule = meetingService.getDeviceSchedule(id, startTime, endTime);
        return ResponseEntity.ok(schedule);
    }
}