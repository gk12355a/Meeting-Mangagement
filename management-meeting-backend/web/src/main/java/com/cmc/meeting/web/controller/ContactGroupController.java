package com.cmc.meeting.web.controller;

import com.cmc.meeting.application.dto.group.ContactGroupDTO;
import com.cmc.meeting.application.dto.group.ContactGroupRequest;
import com.cmc.meeting.application.port.service.ContactGroupService;
import com.cmc.meeting.domain.port.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contact-groups")
@Tag(name = "Contact Group API", description = "API quản lý nhóm liên hệ cá nhân")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()") // Chỉ cần đăng nhập
public class ContactGroupController {

    private final ContactGroupService groupService;
    private final UserRepository userRepository; // Dùng để lấy current user

    public ContactGroupController(ContactGroupService groupService, UserRepository userRepository) {
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Lấy tất cả nhóm liên hệ của tôi (BS-20.3)")
    public ResponseEntity<List<ContactGroupDTO>> getMyGroups(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = getUserId(userDetails);
        return ResponseEntity.ok(groupService.getMyContactGroups(currentUserId));
    }

    @PostMapping
    @Operation(summary = "Tạo một nhóm liên hệ mới (BS-20.3)")
    public ResponseEntity<ContactGroupDTO> createGroup(
            @Valid @RequestBody ContactGroupRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = getUserId(userDetails);
        ContactGroupDTO createdGroup = groupService.createContactGroup(request, currentUserId);
        return new ResponseEntity<>(createdGroup, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật một nhóm (thêm/xóa thành viên) (BS-20.3)")
    public ResponseEntity<ContactGroupDTO> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody ContactGroupRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = getUserId(userDetails);
        return ResponseEntity.ok(groupService.updateContactGroup(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa một nhóm liên hệ (BS-20.3)")
    public ResponseEntity<?> deleteGroup(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = getUserId(userDetails);
        groupService.deleteContactGroup(id, currentUserId);
        return ResponseEntity.ok("Đã xóa nhóm liên hệ thành công.");
    }

    // Helper lấy ID từ Token
    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại từ token"))
                .getId();
    }
}