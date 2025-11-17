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
import com.cmc.meeting.application.port.service.MeetingService;
import com.cmc.meeting.application.dto.meeting.MeetingCancelRequest; 
import com.cmc.meeting.domain.model.Meeting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final MeetingRepository meetingRepository;
    
    // === TIÊM (INJECT) SERVICE MỚI ===
    private final MeetingService meetingService;

    // === CẬP NHẬT CONSTRUCTOR ===
    public AdminUserServiceImpl(UserRepository userRepository,
            ModelMapper modelMapper,
            MeetingRepository meetingRepository,
            MeetingService meetingService) { // <-- Thêm service
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.meetingRepository = meetingRepository;
        this.meetingService = meetingService; // <-- Thêm service
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

    /**
     * (US-18) Vô hiệu hóa User VÀ hủy các cuộc họp tương lai của họ.
     */
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
            // (Giả sử bạn đã thêm hàm 'countAdmins' vào UserRepository)
            // long adminCount = userRepository.countAdmins(); 
            // if (adminCount <= 1) {
            //     throw new PolicyViolationException("Không thể xóa Admin cuối cùng của hệ thống.");
            // }
            // (Tạm thời dùng logic cũ của bạn nếu chưa có hàm countAdmins)
             long adminCount = userRepository.findAll().stream()
                    .filter(user -> user.getRoles().contains(Role.ROLE_ADMIN) && user.isActive())
                    .count();
            if (adminCount <= 1) {
                throw new PolicyViolationException("Không thể xóa Admin cuối cùng của hệ thống.");
            }
        }

        // 3. (LOGIC MỚI) TÌM VÀ HỦY CÁC CUỘC HỌP LIÊN QUAN
        log.info("Đang tìm các cuộc họp tương lai do User ID: {} tổ chức...", userIdToDelete);
        
        List<Meeting> futureMeetings = meetingRepository
                .findFutureMeetingsByOrganizerId(userIdToDelete, LocalDateTime.now());

        if (!futureMeetings.isEmpty()) {
            log.warn("User ID: {} đang tổ chức {} cuộc họp. Sẽ tiến hành hủy...", 
                     userIdToDelete, futureMeetings.size());
        
            
            // 1. Tạo DTO rỗng
            MeetingCancelRequest reason = new MeetingCancelRequest();
            // 2. Dùng hàm setter
            reason.setReason(
                "Người tổ chức cuộc họp (" + userToDelete.getFullName() + ") đã bị vô hiệu hóa khỏi hệ thống."
            );
            
            // ==========================================================
            
            for (Meeting meeting : futureMeetings) {
                try {
                    // Gọi MeetingService để hủy họp VÀ gửi thông báo
                    meetingService.cancelMeeting(meeting.getId(), reason, currentAdminId);
                } catch (Exception e) {
                    log.error("Lỗi khi tự động hủy meeting ID {}: {}", meeting.getId(), e.getMessage());
                }
            }
        } else {
            log.info("User ID: {} không có cuộc họp tương lai nào.", userIdToDelete);
        }

        // 4. (LOGIC MỚI) VÔ HIỆU HÓA USER (SOFT DELETE)
        userToDelete.setActive(false); 
        userToDelete.setUsername(userToDelete.getUsername() + "_disabled_" + System.currentTimeMillis());
        
        userRepository.save(userToDelete);
        
        log.info("Đã vô hiệu hóa (soft delete) User ID: {} thành công.", userIdToDelete);
    }
}