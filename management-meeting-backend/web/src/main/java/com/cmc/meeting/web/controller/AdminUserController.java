package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.admin.AdminUserDTO;
import com.cmc.meeting.application.dto.admin.AdminUserUpdateRequest;
import com.cmc.meeting.application.port.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin: User Management API", description = "API cho Admin quản lý người dùng")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')") // <-- KHÓA TOÀN BỘ CONTROLLER
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả người dùng (Admin only)")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật roles/status của user (Admin only)")
    public ResponseEntity<AdminUserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }
}