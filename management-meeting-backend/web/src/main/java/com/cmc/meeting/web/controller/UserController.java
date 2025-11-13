package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.request.UserProfileUpdateRequest;
import com.cmc.meeting.application.dto.response.UserDTO; // Dùng DTO đơn giản
import com.cmc.meeting.application.port.service.UserService; // Service mới
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
// import com.cmc.meeting.application.dto.request.UserProfileUpdateRequest;
import java.util.List;
@RestController
@RequestMapping("/api/v1/users") // Endpoint mới, không có /admin
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * API MỚI (US-4): Tìm kiếm người dùng để mời họp.
     * Quyền: Bất kỳ ai đã đăng nhập (ROLE_USER).
     *
     * @param query Chuỗi tìm kiếm (tên hoặc email/username)
     * @return Danh sách UserDTO (chỉ id, fullName)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')") // Đảm bảo an toàn
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        List<UserDTO> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')") // Yêu cầu đã đăng nhập
    public ResponseEntity<UserDTO> updateUserProfile(
            @RequestBody UserProfileUpdateRequest request,
            Authentication authentication) {
        
        // Lấy username (email) từ token
        String currentUsername = ((UserDetails) authentication.getPrincipal()).getUsername();
        
        UserDTO updatedUser = userService.updateUserProfile(currentUsername, request);
        return ResponseEntity.ok(updatedUser);
    }
}