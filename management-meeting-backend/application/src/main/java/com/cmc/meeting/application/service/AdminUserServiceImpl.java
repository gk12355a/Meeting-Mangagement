package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.admin.AdminUserDTO;
import com.cmc.meeting.application.dto.admin.AdminUserUpdateRequest;
import com.cmc.meeting.application.port.service.AdminUserService;
import com.cmc.meeting.domain.exception.PolicyViolationException;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import com.cmc.meeting.domain.port.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cmc.meeting.domain.model.Role;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
private final MeetingRepository meetingRepository; // BỔ SUNG

    // CẬP NHẬT CONSTRUCTOR
    public AdminUserServiceImpl(UserRepository userRepository, 
                                ModelMapper modelMapper, 
                                MeetingRepository meetingRepository) { // Bổ sung
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.meetingRepository = meetingRepository; // Bổ sung
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
            .map(user -> modelMapper.map(user, AdminUserDTO.class))
            .collect(Collectors.toList());
    }

    @Override
    public AdminUserDTO updateUser(Long userId, AdminUserUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user: " + userId));

        // Cập nhật quyền và trạng thái
        user.setRoles(request.getRoles());
        user.setActive(request.getIsActive());

        User updatedUser = userRepository.save(user);
        return modelMapper.map(updatedUser, AdminUserDTO.class);
    }
    // BỔ SUNG: (US-18)
    @Override
    public void deleteUser(Long userIdToDelete, Long currentAdminId) {
        // 1. Kiểm tra an toàn: Không thể tự xóa chính mình
        if (userIdToDelete.equals(currentAdminId)) {
            throw new PolicyViolationException("Admin không thể tự xóa chính mình.");
        }

        User userToDelete = userRepository.findById(userIdToDelete)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user: " + userIdToDelete));

        // 2. Kiểm tra an toàn: Không thể xóa Admin cuối cùng
        if (userToDelete.getRoles().contains(Role.ROLE_ADMIN)) {
            long adminCount = userRepository.findAll().stream()
                    .filter(user -> user.getRoles().contains(Role.ROLE_ADMIN))
                    .count();
            if (adminCount <= 1) {
                throw new PolicyViolationException("Không thể xóa Admin cuối cùng của hệ thống.");
            }
        }

        // 3. Kiểm tra ràng buộc: Không thể xóa người đang tổ chức họp
        if (meetingRepository.existsByOrganizerId(userIdToDelete)) {
            throw new PolicyViolationException("Không thể xóa user vì họ đang tổ chức các cuộc họp. Vui lòng gán lại các cuộc họp đó trước.");
        }

        // 4. (Xử lý nâng cao): Xóa các ràng buộc khóa ngoại (Foreign Key)
        // Ví dụ: Xóa các nhóm liên hệ (Contact Groups) mà user này sở hữu
        // (Code này sẽ cần 'ContactGroupRepository')
        // contactGroupRepository.deleteAllByOwnerId(userIdToDelete);

        // 5. Xóa User
        userRepository.delete(userToDelete);
    }
}