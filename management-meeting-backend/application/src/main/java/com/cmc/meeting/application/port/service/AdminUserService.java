package com.cmc.meeting.application.port.service;

import com.cmc.meeting.application.dto.admin.AdminUserDTO;
import com.cmc.meeting.application.dto.admin.AdminUserUpdateRequest;
import java.util.List;

public interface AdminUserService {
    List<AdminUserDTO> getAllUsers();
    AdminUserDTO updateUser(Long userId, AdminUserUpdateRequest request);
    void deleteUser(Long userIdToDisable, Long currentAdminId);
}