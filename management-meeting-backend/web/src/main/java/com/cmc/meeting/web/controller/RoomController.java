package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.room.RoomDTO;
import com.cmc.meeting.application.dto.room.RoomRequest;
import com.cmc.meeting.application.port.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.cmc.meeting.application.port.service.MeetingService; // <-- IMPORT MỚI
import com.cmc.meeting.application.dto.response.BookedSlotDTO; // <-- IMPORT MỚI
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Quan trọng
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@Tag(name = "Room API", description = "API Quản lý phòng họp (Admin) và Tra cứu (User)")
@SecurityRequirement(name = "bearerAuth") // Báo Swagger API này cần token
public class RoomController {

    private final RoomService roomService;
    private final MeetingService meetingService;

    public RoomController(RoomService roomService, MeetingService meetingService) {
        this.roomService = roomService;
        this.meetingService = meetingService;
    }

    /**
     * API Lấy danh sách tất cả phòng họp (US-7)
     * Cho phép tất cả user đã đăng nhập
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả phòng họp (Cho tất cả user)")
    @PreAuthorize("isAuthenticated()") // Chỉ cần đăng nhập là được
    public ResponseEntity<List<RoomDTO>> getAllRooms() {
        List<RoomDTO> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    /**
     * API Tạo phòng họp mới (US-11)
     * Chỉ Admin
     */
    @PostMapping
    @Operation(summary = "Tạo phòng")
    @PreAuthorize("hasRole('ADMIN')") // <-- CHỈ ADMIN MỚI ĐƯỢC GỌI
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody RoomRequest request) {
        RoomDTO createdRoom = roomService.createRoom(request);
        return new ResponseEntity<>(createdRoom, HttpStatus.CREATED);
    }

    /**
     * API Cập nhật phòng họp (US-11)
     * Chỉ Admin
     */
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin phòng họp (Chỉ Admin)")
    @PreAuthorize("hasRole('ADMIN')") // <-- CHỈ ADMIN MỚI ĐƯỢC GỌI
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long id, 
                                            @Valid @RequestBody RoomRequest request) {
        RoomDTO updatedRoom = roomService.updateRoom(id, request);
        return ResponseEntity.ok(updatedRoom);
    }

    /**
     * API Xóa phòng họp (US-11)
     * Chỉ Admin
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa một phòng họp (Chỉ Admin)")
    @PreAuthorize("hasRole('ADMIN')") // <-- CHỈ ADMIN MỚI ĐƯỢC GỌI
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok("Đã xóa phòng họp thành công.");
    }
    /**
     * API Gợi ý phòng họp (US-26)
     * Lấy danh sách phòng trống theo thời gian và sức chứa
     */
    @GetMapping("/available")
    @Operation(summary = "Gợi ý phòng họp (theo thời gian & sức chứa)")
    @PreAuthorize("isAuthenticated()") // Chỉ cần đăng nhập
    public ResponseEntity<List<RoomDTO>> getAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam int capacity) {

        List<RoomDTO> rooms = roomService.findAvailableRooms(startTime, endTime, capacity);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}/meetings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BookedSlotDTO>> getRoomSchedule(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        // Dòng này sẽ hết lỗi vì 'meetingService' đã được tiêm (inject)
        List<BookedSlotDTO> schedule = meetingService.getRoomSchedule(id, startTime, endTime);
        return ResponseEntity.ok(schedule);
    }
}