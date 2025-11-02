package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.admin.AdminUserDTO;
import com.cmc.meeting.application.dto.admin.AdminUserUpdateRequest;
import com.cmc.meeting.application.port.service.AdminUserService;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public AdminUserServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
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
}