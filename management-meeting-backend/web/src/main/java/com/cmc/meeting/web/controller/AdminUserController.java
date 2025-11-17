package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.admin.AdminUserDTO;
import com.cmc.meeting.application.dto.admin.AdminUserUpdateRequest;
import com.cmc.meeting.application.dto.request.AdminUserCreationRequest;
import com.cmc.meeting.application.port.service.AdminUserService;
import com.cmc.meeting.domain.port.repository.UserRepository;
import com.cmc.meeting.domain.model.User; // <-- Import User
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin: User Management API", description = "API cho Admin quản lý người dùng")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserRepository userRepository; // Cần thiết cho helper

    public AdminUserController(AdminUserService adminUserService, 
                               UserRepository userRepository) {
        this.adminUserService = adminUserService;
        this.userRepository = userRepository;
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa (Vô hiệu hóa) một user (Admin only)")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // Lấy ID của Admin đang thực thi
        Long currentAdminId = getUserId(userDetails);
        
        // Gọi logic nghiệp vụ mới
        adminUserService.deleteUser(id, currentAdminId);
        
        return ResponseEntity.ok("Đã vô hiệu hóa user và hủy các cuộc họp liên quan thành công.");
    }
    
    // Helper lấy ID từ UserDetails
    private Long getUserId(UserDetails userDetails) {
        // (Chúng ta giả định CustomUserDetails không có getId(), 
        //  nên dùng cách an toàn là tìm bằng username)
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại từ token"));
        return user.getId();
    }

    @PostMapping
    @Operation(summary = "Tạo người dùng mới (Admin only)")
    public ResponseEntity<AdminUserDTO> createUser(
            @Valid @RequestBody AdminUserCreationRequest request) {
        
        AdminUserDTO newUser = adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }
    
}