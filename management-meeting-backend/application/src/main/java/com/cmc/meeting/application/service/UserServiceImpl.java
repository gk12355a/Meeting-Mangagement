package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.request.UserProfileUpdateRequest;
import com.cmc.meeting.application.dto.response.UserDTO;
import com.cmc.meeting.application.port.service.UserService;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional // Đa số là readOnly
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;


    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> searchUsers(String query) {
        // Gọi hàm mới từ Repository Port
        List<User> users = userRepository.searchByNameOrUsername(query);

        // Ánh xạ sang DTO an toàn (UserDTO chỉ có id, fullName)
        return users.stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .collect(Collectors.toList());
    }
    @Override
    @Transactional // (Bỏ readOnly vì đây là hàm Cập nhật)
    public UserDTO updateUserProfile(String username, UserProfileUpdateRequest request) {
        
        // 1. Tìm người dùng bằng username (email) từ token
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng: " + username));

        // 2. Cập nhật thông tin
        // (Giả sử User domain model có hàm setFullName)
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        // 3. Lưu lại vào CSDL (Chúng ta cần hàm 'save' trong UserRepository)
        User updatedUser = userRepository.save(user);

        // 4. Trả về DTO an toàn
        return modelMapper.map(updatedUser, UserDTO.class);
    }
}