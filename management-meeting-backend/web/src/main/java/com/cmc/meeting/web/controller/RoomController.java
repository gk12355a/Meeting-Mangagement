package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.room.RoomDTO;
import com.cmc.meeting.application.dto.room.RoomRequest;
import com.cmc.meeting.application.port.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Quan trọng
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@Tag(name = "Room API", description = "API Quản lý phòng họp (Admin) và Tra cứu (User)")
@SecurityRequirement(name = "bearerAuth") // Báo Swagger API này cần token
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
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
    @Operation(summary = "Tạo phòng họp mới (Chỉ Admin)")
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
}